package bars4j;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.bars4j.encode.Code39Encoder;
import org.bars4j.encode.Code93Encoder;
import org.bars4j.encode.EAN13Encoder;
import org.bars4j.encode.InvalidAtributeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BarcodeEncoderTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() throws InvalidAtributeException {
		System.out.println(Arrays.toString(EAN13Encoder.getInstance().encode("7891827361253")));
		fail("Not yet implemented");
	}

}
