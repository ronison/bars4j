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


/**
 * TODO: Description.
 * 
 * @author Fl�vio Sampaio
 * @since 0.1
 */
public class UPCAEncoder extends EANEncoder {
    
    private static UPCAEncoder instance;
   
    private UPCAEncoder(){
        
    }
    
    public static UPCAEncoder getInstance(){
        if(instance == null){
            instance = new UPCAEncoder();
        }
        return instance;
    }
	
	
	public BarSet[] encode(String texto) throws InvalidAtributeException {
		if( texto == null || texto.length() != 12 ){
			throw new InvalidAtributeException("[UPCA] Invalid text length (" + (texto == null ? "null" : texto.length()) + "): 12 digits are required.");
		}
		try{
			return EAN13Encoder.getInstance().encode("0" + texto);
		} catch( InvalidAtributeException tiexc ){
			throw new InvalidAtributeException("[UPCA] Only numbers suported.", tiexc);
		}
	}

	/* (non-Javadoc)
	 * @see org.jbarcode.BarcodeEncoder#calcularDV(java.lang.String)
	 */
	public String computeCheckSum(String texto) throws InvalidAtributeException {
		return super.computeCheckSum("0" + texto);
	}
    
    public String convertUPCAtoUPCE(String upca) throws InvalidAtributeException{
        if(upca == null || (upca.length() != 11 && upca.length() != 12)){
            throw new InvalidAtributeException("[UPCA] Invalid text length (" + (upca == null ? "null" : upca.length()) + ").");
        }
        StringBuffer result = new StringBuffer();
        result.append(upca.charAt(0));
        if( upca.charAt(0) != '0' && upca.charAt(0) != '1' ){
            throw new InvalidAtributeException("[UPCA] Invalid Number System,  only 0 & 1 are valid (" + upca.charAt(0) + ").");
        } else if( "000".equals(upca.substring(3, 6)) || "100".equals(upca.substring(3, 6)) || "200".equals(upca.substring(3, 6)) ){
            // Compression case 0/1/2 additionally requires product digits I1 and I2 to be zero.
            if( upca.charAt(6) != '0' || upca.charAt(7) != '0' ){
                throw new InvalidAtributeException("[UPCA] Code cannot be compressed to UPC-E: product digits I1/I2 must be 00.");
            }
            result.append(upca.substring(1, 3));
            result.append(upca.substring(8, 11));
            result.append(upca.charAt(3));
        } else if( "00".equals(upca.substring(4, 6)) ){
            // Compression case 3 additionally requires product digits I1..I3 to be zero.
            if( upca.charAt(6) != '0' || upca.charAt(7) != '0' || upca.charAt(8) != '0' ){
                throw new InvalidAtributeException("[UPCA] Code cannot be compressed to UPC-E: product digits I1..I3 must be 000.");
            }
            result.append(upca.substring(1, 4));
            result.append(upca.substring(9, 11));
            result.append('3');
        } else if( upca.charAt(5) == '0' ){
            // Compression case 4 additionally requires product digits I1..I4 to be zero.
            if( upca.charAt(6) != '0' || upca.charAt(7) != '0' || upca.charAt(8) != '0' || upca.charAt(9) != '0' ){
                throw new InvalidAtributeException("[UPCA] Code cannot be compressed to UPC-E: product digits I1..I4 must be 0000.");
            }
            result.append(upca.substring(1, 5));
            result.append(upca.charAt(10));
            result.append('4');
        } else if( upca.charAt(10) >= '5' && upca.charAt(10) <= '9' ){
            result.append(upca.substring(1, 6));
            result.append(upca.charAt(10));
        } else {
            throw new InvalidAtributeException("[UPCA] Invalid code.");
        }
        if(upca.length() == 12){
            result.append(upca.charAt(11));
        }
        return result.toString();
    }
    
    public String toString(){
        return "UPC-A";
    }
}
