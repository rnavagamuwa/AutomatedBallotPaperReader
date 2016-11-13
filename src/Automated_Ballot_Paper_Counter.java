import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Automated_Ballot_Paper_Counter {

    private JPanel panel1;
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
    private JTextField ballotPaperDir;
    private JButton button1;
    private JButton countVotesForAllButton;
    private JLabel winningParty;
    private JLabel errorLbl;
    private ArrayList<PartyVote> votes = new ArrayList<PartyVote>();
    private int numberOfParties = 0;


    public Automated_Ballot_Paper_Counter() {

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


                        VoteCounter t=new VoteCounter(filePathText.getText(),partySymbolDirTxt.getText());
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

        countVotesForAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(ballotPaperDir.getText()!= ""){
                    File dir = new File(ballotPaperDir.getText());
                    File[] directoryListing = dir.listFiles();
                    if (directoryListing != null) {
                        for (File child : directoryListing) {
                            VoteCounter t=new VoteCounter(child.getPath(),partySymbolDirTxt.getText());
                            if(t.isSuccess){
                                JLabel votedPic = new JLabel();
                                votedPic.setSize(240,500);
                                Image dimgV =  t.getOutputImg().getScaledInstance(votedPic.getWidth(), votedPic.getHeight(),Image.SCALE_SMOOTH);
                                votedPic.setIcon(new ImageIcon(dimgV));
                                setPartyVotes(t.getPartySymbol());

                            }else{
                                errorLbl.setText("Disqualified vote !");
                            }

                        }
                    }
                }
                  Collections.sort(votes);
                  winningParty.setText(votes.get(numberOfParties-1).getName() +" with " + votes.get(numberOfParties-1).getScore() + " votes.");
            }
        });
    }

    private void setPartyVotes(String partyName){
        Iterator<PartyVote> iterator = votes.iterator();
        PartyVote currentParty;
        Boolean isExists = false;
        while (iterator.hasNext()) {
            currentParty = iterator.next();
            if (currentParty.getName()==partyName){
                currentParty.setScore(currentParty.getScore()+1);
                isExists = true;
            }
        }

        if (!isExists){
            votes.add(new PartyVote(1,partyName));
            numberOfParties++;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Automated_Ballot_Paper_Counter");
        frame.setContentPane(new Automated_Ballot_Paper_Counter().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }
}
