import org.opencv.core.Core;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author MaN
 *         on 11/8/2016.
 */
public class MainFrame {

    private JPanel panel1;
    private JButton openBtn;
    private JLabel prefVotesLbl;
    private JLabel mainVoteLbl;
    private JTextField filePathText;
    private JPanel originalImgPanel;
    private JPanel votedImgPanel;
    private JPanel fileChoosePanel;
    private JButton fileBtn;
    private JLabel symbolLbl;
    private JTextField partySymbolDirTxt;
    private JButton symbolDirBtn;
    private JLabel abcLbl;
    private JButton readButton;
    private JLabel errorLbl;

    public MainFrame() {

        openBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                try{
                    BufferedImage myPicture = ImageIO.read(new File(filePathText.getText()));
                    JLabel originalPic = new JLabel();
                    originalPic.setSize(240,450);
                    Image dimg =  myPicture.getScaledInstance(originalPic.getWidth(), originalPic.getHeight(),Image.SCALE_SMOOTH);
                    originalPic.setIcon(new ImageIcon(dimg));
                    originalImgPanel.removeAll();
                    originalImgPanel.add(originalPic);
                    originalImgPanel.revalidate();
                }catch(Exception ex){

                }


            }

        });
        fileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File(filePathText.getText()));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "png", "gif", "jpeg");
                fc.showOpenDialog(fileChoosePanel);
                fc.setFileFilter(filter);
                filePathText.setText(fc.getSelectedFile().toString());
            }
        });
        symbolDirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc;
                if(partySymbolDirTxt.getText()!=""){
                    fc = new JFileChooser(new File(partySymbolDirTxt.getText())){
                        public void approveSelection()
                        {
                            if (getSelectedFile().isFile())
                            {
                                // beep
                                return;
                            }
                            else
                                super.approveSelection();
                        }
                    };;
                }else{
                    fc= new JFileChooser(){
                        public void approveSelection()
                        {
                            if (getSelectedFile().isFile())
                            {
                                // beep
                                return;
                            }
                            else
                                super.approveSelection();
                        }
                    };
                }
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fc.showOpenDialog(fileChoosePanel);
                partySymbolDirTxt.setText(fc.getSelectedFile().toString());
            }
        });
        readButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try{
                    if(filePathText.getText()!= ""){


                        Test t=new Test(filePathText.getText(),partySymbolDirTxt.getText());
                        if(t.isSuccess){
                            JLabel votedPic = new JLabel();
                            votedPic.setSize(240,500);
                            Image dimgV =  t.getOutputImg().getScaledInstance(votedPic.getWidth(), votedPic.getHeight(),Image.SCALE_SMOOTH);
                            votedPic.setIcon(new ImageIcon(dimgV));
                            votedImgPanel.removeAll();
                            votedImgPanel.add(votedPic);
                            votedImgPanel.validate();

                            mainVoteLbl.setText(t.getMainVote()+"");
                            symbolLbl.setText(t.getPartySymbol());
                            String prefVotes="";
                            for(int i:t.getPrefVotes()){
                                prefVotes+=(i+1)+"  ";
                            }
                            prefVotesLbl.setText(prefVotes);
                        }else{
                            errorLbl.setText("Disqualified vote !");
                        }



                    }else{
                        System.out.println("set a path !!!");
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
               // abcLbl.setText("Done.");
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainFrame");
        frame.setContentPane(new MainFrame().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

}
