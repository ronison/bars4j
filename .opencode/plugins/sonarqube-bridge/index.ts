/**
 * Bridge plugin: adapts the V1-style `opencode-sonarqube` package (async function
 * entrypoint, `@opencode-ai/plugin` ^1.x) to the OpenCode V2 plugin API
 * (`Plugin.define` + `ctx.*` domains).
 *
 * The upstream package (npm `opencode-sonarqube`) registers:
 * - a `sonarqube` tool (V1 `tool()` helper)
 * - an `event` hook (session tracking, initial quality check, idle analysis)
 * - `tool.execute.before` / `tool.execute.after` hooks
 * - `experimental.chat.system.transform` (quality-gate status into the system prompt)
 * - `experimental.session.compacting` (quality context survives compaction)
 *
 * V2 equivalents used here:
 * - `ctx.tool.transform`  -> registers the `sonarqube` tool (JSON Schema input)
 * - `ctx.event.subscribe` -> public server event stream
 * - `ctx.tool.hook("execute.before"/"execute.after")`
 * - `ctx.session.hook("context")`   -> system prompt injection
 * - `ctx.session.hook("compaction")` -> compaction context injection
 *
 * The heavy lifting (SonarQube API, config, state) stays in the installed package:
 * we import its default export (the V1 plugin function) and call it with a
 * V1-shaped context, then replay the returned V1 hooks/tools through V2 APIs.
 */
import { Plugin } from "@opencode/plugin"

const V1_PACKAGE = "opencode-sonarqube/dist/index.js"

export default Plugin.define({
  id: "sonarqube",
  async setup(ctx) {
    // Load the V1 plugin implementation from the installed npm package.
    const mod = await import(V1_PACKAGE)
    const v1Factory = mod.default
    if (typeof v1Factory !== "function") {
      throw new Error(
        `opencode-sonarqube: expected the V1 default export to be a function, got ${typeof v1Factory}`,
      )
    }

    // V1 context shape: the upstream plugin uses `client` for session/tui calls.
    // V2's ctx is itself a client, so we pass it through. `directory` comes from
    // the plugin location.
    const v1 = await v1Factory({
      client: ctx,
      directory: ctx.location.directory,
      project: ctx.location.project,
      worktree: undefined,
    })

    // --- Tool registration (V1 `tool` map -> V2 `ctx.tool.transform`) -------
    const v1Tools: Record<string, any> | undefined = (v1 as any).tool
    if (v1Tools) {
      await ctx.tool.transform((editor: any) => {
        for (const [name, def] of Object.entries(v1Tools)) {
          editor.add({
            name,
            description: (def as any).description ?? "",
            // V1 args are zod-ish descriptors; expose them verbatim as the input
            // schema when possible, otherwise accept any object.
            input: (def as any).args ?? {
              type: "object",
              properties: {},
              additionalProperties: true,
            },
            async execute(input: any) {
              // V1 tool executors return a plain string; the V2 contract requires
              // an object shaped like Tool.Result. `content` accepts a plain string
              // without an output schema (`output` would require one).
              const result = await (def as any).execute(input, ctx)
              return typeof result === "string" ? { content: result } : (result ?? { content: "" })
            },
          })
        }
      })
    }

    // --- Event subscription (V1 `event` hook -> V2 `ctx.event.subscribe`) ---
    const v1Event = (v1 as any).event
    const controller = new AbortController()
    if (typeof v1Event === "function") {
      void (async () => {
        try {
          for await (const event of ctx.event.subscribe({ signal: controller.signal })) {
            await v1Event({ event })
          }
        } catch (error) {
          if (!controller.signal.aborted) {
            console.error("sonarqube-bridge: event subscription failed", error)
          }
        }
      })()
    }

    // --- Tool lifecycle hooks ----------------------------------------------
    const v1Before = (v1 as any)["tool.execute.before"]
    const v1After = (v1 as any)["tool.execute.after"]
    if (typeof v1Before === "function") {
      await ctx.tool.hook("execute.before", (event: any) => {
        return v1Before({ tool: event.tool }, event)
      })
    }
    if (typeof v1After === "function") {
      await ctx.tool.hook("execute.after", (event: any) => {
        return v1After({ tool: event.tool }, event)
      })
    }

    // --- System prompt injection (V1 experimental.chat.system.transform) ----
    const v1System = (v1 as any)["experimental.chat.system.transform"]
    if (typeof v1System === "function") {
      await ctx.session.hook("context", (event: any) => {
        const output = { system: event.system ?? [] }
        return v1System({}, output).then(() => {
          // The V1 hook mutates `output.system` in place; V2's context hook
          // reads `event.system`, which is the same array reference.
        })
      })
    }

    // --- Compaction context (V1 experimental.session.compacting) ------------
    const v1Compaction = (v1 as any)["experimental.session.compacting"]
    if (typeof v1Compaction === "function") {
      await ctx.session.hook("compaction", (event: any) => {
        const output = { context: event.context ?? [] }
        return v1Compaction({}, output).then(() => {
          // Same in-place mutation contract as the system transform.
        })
      })
    }

    return () => controller.abort()
  },
})
