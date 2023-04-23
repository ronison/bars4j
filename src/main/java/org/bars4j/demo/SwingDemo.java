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
package org.bars4j.demo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import org.bars4j.BarcodeComponent;
import org.bars4j.BarcodeFactory;
import org.bars4j.encode.InvalidAtributeException;




/**
 * TODO: Description.
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class SwingDemo extends javax.swing.JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private BarcodeComponent jbc;
    private JLabel jLabel2;
    private JLabel jLabel1;
    private JButton jButton2;
    private JButton jButton4;
    private JButton jButton3;
    private JTextField typeText;
    private JTextField jTextField3;
    private JLabel jLabel3;
    private JTextField jTextField2;
    private JButton jButton1;
    private JTextField jTextField1;
    
    int index = 0;

    /**
    * Auto-generated main method to display this JFrame
    */
    public static void main(String[] args) {
        SwingDemo inst = new SwingDemo();
        inst.setVisible(true);
    }
    
    public SwingDemo() {
        super();
        initGUI();
    }
    
    private void initGUI() {
        try {
 
            setSize(400, 300);
            {
                
                {
                    getContentPane().setLayout(null);
                    this.setTitle("JBarcode - Swing Demo");
                    jbc = new BarcodeComponent(BarcodeFactory.getInstance().createEAN13());
                    getContentPane().add(jbc);
                    jbc.setBounds(21, 7, 357, 154);
                    jbc.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                    jbc.setText("789563251045");
                    jbc.setBarHeight(17);
                    jbc.setCheckSum("4");
                    jbc.setXDimension(0.264583333);
                    jbc.setWideRatio(2.0);
                }
                {
                    jTextField1 = new JTextField();
                    getContentPane().add(jTextField1);
                    jTextField1.setBounds(21, 189, 147, 28);
                    jTextField1.setText("789563251045");
                }
                {
                    jButton1 = new JButton();
                    getContentPane().add(jButton1);
                    jButton1.setText("Ok");
                    jButton1.setBounds(175, 189, 56, 28);
                    jButton1.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent evt) {
                            try {
                                jbc.setText(jTextField1.getText());
                            } catch (InvalidAtributeException exc) {
                                JOptionPane.showMessageDialog(null, exc.getMessage(), "Falha", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                }
                {
                    jTextField2 = new JTextField();
                    getContentPane().add(jTextField2);
                    jTextField2.setText("17");
                    jTextField2.setBounds(273, 189, 49, 28);
                }
                {
                    jButton2 = new JButton();
                    getContentPane().add(jButton2);
                    jButton2.setText("Set");
                    jButton2.setBounds(294, 224, 63, 28);
                    jButton2.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent evt) {
                            jbc.setBarHeight(Double.parseDouble(jTextField2.getText()));
                            try {
                                jbc.setXDimension(Double.parseDouble(jTextField3.getText()));
                            } catch (NumberFormatException exc) {
                                exc.printStackTrace();
                            } catch (InvalidAtributeException exc) {
                                exc.printStackTrace();
                            }
                        }
                    });
                }
                {
                    jLabel1 = new JLabel();
                    getContentPane().add(jLabel1);
                    jLabel1.setText("Text");
                    jLabel1.setBounds(21, 168, 63, 28);
                }
                {
                    jLabel2 = new JLabel();
                    getContentPane().add(jLabel2);
                    jLabel2.setText("Height");
                    jLabel2.setBounds(273, 168, 63, 28);
                }
                {
                    jLabel3 = new JLabel();
                    getContentPane().add(jLabel3);
                    jLabel3.setText("Width");
                    jLabel3.setBounds(329, 168, 63, 28);
                }
                {
                    jTextField3 = new JTextField();
                    getContentPane().add(jTextField3);
                    jTextField3.setText("0.264583333");
                    jTextField3.setBounds(329, 189, 49, 28);
                }
                {
                    typeText = new JTextField();
                    getContentPane().add(typeText);
                    typeText.setBounds(70, 224, 112, 28);
                    typeText.setEditable(false);
                    typeText.setText(jbc.getBarcode().toString());
                }
                {
                    jButton3 = new JButton();
                    getContentPane().add(jButton3);
                    jButton3.setText("<");
                    jButton3.setBounds(21, 224, 49, 28);
                    jButton3.setEnabled(false);
                    jButton3.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent evt) {
                            if(jButton3.isEnabled()){
                                setEncoder(--index);
                            }
                        }
                    });   
                }
                {
                    jButton4 = new JButton();
                    getContentPane().add(jButton4);
                    jButton4.setText(">");
                    jButton4.setBounds(182, 224, 49, 28);
                    jButton4.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent evt) {
                            if(jButton4.isEnabled()){
                                setEncoder(++index);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Falha", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.JFrame#processWindowEvent(java.awt.event.WindowEvent)
     */
    protected void processWindowEvent(WindowEvent e) {
       super.processWindowEvent(e);
       if(e.getID() == WindowEvent.WINDOW_CLOSING){
           System.exit(0);
       }
    }
    
    void setEncoder(int val){
        switch (val) {
        case 0:
            setTextToSize(12);
            jbc.setBarcode(BarcodeFactory.getInstance().createEAN13());
            jButton3.setEnabled(false);
            break;
            
        case 1:
        	jbc.setBarcode(BarcodeFactory.getInstance().createUPCA());
            setTextToSize(11);
            jButton3.setEnabled(true);
            break;
            
        case 2:
        	jbc.setBarcode(BarcodeFactory.getInstance().createEAN8());
            setTextToSize(7);
            break;
            
        case 3:
        	jbc.setBarcode(BarcodeFactory.getInstance().createUPCE());
            setTextToSize(7);
            break;
            
        case 4:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCodabar());
            break;
        
        case 5:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode11());
            break;
            
        case 6:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode39());
            break;
            
        case 7:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode39Extended());
            break;
            
        case 8:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode93());
            break;
            
        case 9:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode93Extended());
            break;
            
        case 10:
        	jbc.setBarcode(BarcodeFactory.getInstance().createCode128());
            break;
            
        case 11:
        	jbc.setBarcode(BarcodeFactory.getInstance().createMSIPlessey());
            break;
            
        case 12:
        	jbc.setBarcode(BarcodeFactory.getInstance().createStandard2of5());
            break;
            
        case 13:
        	jbc.setBarcode(BarcodeFactory.getInstance().createIATA2of5());
            break;
            
        case 14:
        	jbc.setBarcode(BarcodeFactory.getInstance().createInterleaved2of5());
            jTextField2.setText("17");
            jButton4.setEnabled(true);
            break;
            
        case 15:
        	jbc.setBarcode(BarcodeFactory.getInstance().createPostNet());
            jTextField2.setText("6");
            jButton4.setEnabled(false);
            break;

        default:
            break;
        }
        typeText.setText(jbc.getBarcode().toString());
        jTextField1.setText(jbc.getText());
    }
    
    void setTextToSize(int size){
        String text = jbc.getText();
        try {
            if(text.length() > size){
                jbc.setText(text.substring(0, size));
            } else {
                String result = text;
                while(result.length() < size){
                    result += "0";
                }
                jbc.setText(result);
            }
        } catch (InvalidAtributeException exc) {
        }
    }

}
