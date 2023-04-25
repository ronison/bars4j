/* ================================================================
 * Bars4J : Java Barcode Library
 * ================================================================
 *
 * Project Info:  https://github.com/ronison/bars4j
 * Project Lead:  Flavio Sampaio (contato@ronison.com.br);
 *
 * (C) Copyright 2005, by Favio Sampaio
 *
 * This library is free software; you can redistribute it and/or modify it underthe terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.bars4j.encode;

public class IATA2of5Encoder extends Standard2of5Encoder {

	private static IATA2of5Encoder instance;

	public IATA2of5Encoder() {
		super.START_CHAR = new BarSet("0000");
		super.STOP_CHAR = new BarSet("1000");
	}
	
	public static IATA2of5Encoder getInstance(){
        if(instance == null){
            instance = new IATA2of5Encoder();
        }
        return instance;
    }
    
    public String toString(){
        return "IATA 2of5";
    }
}
