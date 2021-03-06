/*
 * This file is part of Greta.
 *
 * Greta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Greta.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package greta.core.utilx.gui;

import greta.core.util.log.LogFile;
import greta.core.util.log.LogOutput;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 *
 * @author Andre-Marie Pez
 */
public class LogFileAccessor extends javax.swing.JFrame implements LogOutput{

    private LogFile logFile;
    private String currentFile=null;

    /** Creates new form LogFileAccessor */
    public LogFileAccessor() {
        initComponents();
    }

    public String getLogFile(){
        if(currentFile==null)
            return "";
        return currentFile.replaceAll("\\\\", "/"); //UNIX compatible
    }

    public void setLogFile(String fileName){
        setLogFileWithoutGuiUpdate(fileName);
        if(currentFile!=null){
            this.jTextField1.setText(currentFile);
            this.jFileChooser1.setCurrentDirectory(new File(currentFile));
        }
    }

    private void setLogFileWithoutGuiUpdate(String fileName){
        if(fileName==null || fileName.isEmpty()){
            logFile = null;
            currentFile = null;
        }
        else{
            File newfile = new File(fileName);
            try {
                String canonicalname = newfile.getCanonicalPath();
                boolean changepath = false;
                if(currentFile!=null){
                    try {
                        if( ! canonicalname.equals((new File(currentFile)).getCanonicalPath())){
                            changepath = true; // new file
                        }
                    } catch (IOException ex) {
                        changepath = true; // error in current file
                    }
                }
                else{
                    changepath = true; // current file is null
                }
                currentFile = fileName;
                if(changepath){
                    logFile = new LogFile(currentFile);
                }
            } catch (IOException ex) {
                setLogFile(null);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jFileChooser1.setCurrentDirectory(new File("./"));
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new greta.core.utilx.gui.ToolBox.LocalizedJButton("GUI.open");

        jPanel1.setPreferredSize(new java.awt.Dimension(50, 50));

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        setLogFile(jTextField1.getText());
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jFileChooser1.setLocale(Locale.getDefault());
        jFileChooser1.updateUI();
        if(jFileChooser1.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION){
            File file = jFileChooser1.getSelectedFile();
            this.jTextField1.setText(file.getPath());
            setLogFile(jTextField1.getText());
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables

    public void onDebug(String string) {
        if(logFile!=null)logFile.onDebug(string);
    }

    public void onInfo(String string) {
        if(logFile!=null)logFile.onInfo(string);
    }

    public void onWarning(String string) {
        if(logFile!=null)logFile.onWarning(string);
    }

    public void onError(String string) {
        if(logFile!=null)logFile.onError(string);
    }

}
