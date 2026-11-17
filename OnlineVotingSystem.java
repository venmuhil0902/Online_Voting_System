import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;

public class OnlineVotingSystem extends JFrame {
    static class Voter implements Serializable {
        String id, name, pass;
        boolean voted;
        Voter(String i, String n, String p) { id=i; name=n; pass=p; }
    }
    Map<String,Voter> voters = new LinkedHashMap<>();
    int[] votes = new int[3];
    String[] candidates = {"Candidate A", "Candidate B", "Candidate C"};
    Voter current;
    boolean adminSession = false;
    final String FILE = "voting_compact.dat";
    final Color NAVY = new Color(14,30,57), TEAL = new Color(0,145,155);
    final Color BG = new Color(242,246,251), INK = new Color(30,48,74);

    static class RoundedPanel extends JPanel {
        int radius;
        RoundedPanel(Color color, int r) { setBackground(color); setOpaque(false); radius=r; }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
            g2.dispose(); super.paintComponent(g);
        }
    }
    OnlineVotingSystem() {
        setTitle("Online Voting System | Election Portal");
        setSize(1050,700); setMinimumSize(new Dimension(800,550));
        setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null);
        load(); home(); setVisible(true);
    }
    JLabel text(String value, int size, boolean bold) {
        JLabel l=new JLabel(value); l.setForeground(INK);
        l.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size)); return l;
    }
    JButton button(String name, Runnable action) {
        JButton b=new JButton(name); b.setBackground(TEAL); b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI",Font.BOLD,14)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(12,18,12,18));
        b.addActionListener(e->action.run()); return b;
    }
    JPanel card() {
        JPanel p=new RoundedPanel(Color.WHITE,20);
        p.setLayout(new BorderLayout(14,14)); p.setBorder(new EmptyBorder(22,24,22,24));
        return p;
    }
    JPanel stack() {
        JPanel p=new JPanel(); p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); return p;
    }
    void addLine(JPanel p, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(550,58));
        p.add(c); p.add(Box.createVerticalStrut(14));
    }
    JTextField field(String title) {
        JTextField f=new JTextField(22); f.setFont(new Font("Segoe UI",Font.PLAIN,15));
        f.setBorder(BorderFactory.createTitledBorder(title)); return f;
    }
    JPasswordField password() {
        JPasswordField f=new JPasswordField(22);
        f.setBorder(BorderFactory.createTitledBorder("Password")); return f;
    }
    void message(String s) { JOptionPane.showMessageDialog(this,s); }
    void display(JPanel root) { setContentPane(root); revalidate(); repaint(); }

    JPanel layout(String heading, boolean admin) {
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(BG);
        JPanel header=new JPanel(new BorderLayout()); header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(14,22,14,22));
        JLabel logo=text("●  ONLINE VOTING SYSTEM",19,true);
        logo.setForeground(Color.WHITE); header.add(logo,BorderLayout.WEST);
        JPanel links=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); links.setOpaque(false);
        if(admin) links.add(button("Logout",()->{adminSession=false;home();}));
        else {
            links.add(button("Home",this::home));
            links.add(button("Voter Login",()->login(false)));
            links.add(button("Admin Login",()->login(true)));
        }
        header.add(links,BorderLayout.EAST); root.add(header,BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout()); center.setBackground(BG);
        if(admin) {
            JPanel side=new JPanel(); side.setBackground(NAVY);
            side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));
            side.setBorder(new EmptyBorder(20,12,15,12));
            side.setPreferredSize(new Dimension(205,0));
            sideLink(side,"Dashboard",this::dashboard);
            sideLink(side,"Register Voter",this::register);
            sideLink(side,"Import CSV",this::importCSV);
            sideLink(side,"Registered Voters",this::listVoters);
            sideLink(side,"Manage Votes",this::manage);
            sideLink(side,"Results",this::results);
            center.add(side,BorderLayout.WEST);
        }
        JPanel content=new JPanel(new BorderLayout(16,16)); content.setBackground(BG);
        content.setBorder(new EmptyBorder(22,28,28,28));
        content.add(text(heading,25,true),BorderLayout.NORTH);
        center.add(content,BorderLayout.CENTER); root.add(center,BorderLayout.CENTER);
        return root;
    }
    void sideLink(JPanel side,String name,Runnable action) {
        JButton b=button(name,action); b.setBackground(new Color(23,68,91));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(180,45)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(b); side.add(Box.createVerticalStrut(10));
    }
    JPanel body(JPanel root) {
        JPanel center=(JPanel)root.getComponent(1);
        return (JPanel)center.getComponent(center.getComponentCount()-1);
    }
    JPanel banner(String title,String subtitle) {
        JPanel p=new RoundedPanel(new Color(18,105,132),22);
        p.setLayout(new GridLayout(2,1,0,5)); p.setBorder(new EmptyBorder(20,25,20,25));
        JLabel a=text(title,25,true), b=text(subtitle,14,false);
        a.setForeground(Color.WHITE); b.setForeground(Color.WHITE);
        p.add(a); p.add(b); return p;
    }
    JPanel stat(String title,int value) {
        JPanel p=card(); p.add(text(title,15,false),BorderLayout.NORTH);
        JLabel n=text(""+value,31,true); n.setForeground(TEAL);
        p.add(n,BorderLayout.CENTER); return p;
    }
    void centered(JPanel root,JPanel panel) {
        JPanel wrap=new JPanel(new GridBagLayout()); wrap.setOpaque(false);
        panel.setPreferredSize(new Dimension(480,panel.getPreferredSize().height));
        wrap.add(panel); body(root).add(wrap,BorderLayout.CENTER); display(root);
    }
    int votedCount() { int n=0; for(Voter v:voters.values())if(v.voted)n++; return n; }
    void home() {
        JPanel root=layout("Dashboard",false), main=new JPanel(new BorderLayout(18,18));
        main.setOpaque(false);
        main.add(banner("Welcome to Online Voting","Secure  •  Simple  •  Digital"),BorderLayout.NORTH);
        JPanel middle=new JPanel(new GridLayout(2,1,18,18)); middle.setOpaque(false);
        JPanel stats=new JPanel(new GridLayout(1,3,18,18)); stats.setOpaque(false);
        stats.add(stat("Registered Voters",voters.size()));
        stats.add(stat("Votes Cast",votedCount()));
        stats.add(stat("Pending Voters",voters.size()-votedCount()));
        JPanel actions=card(), row=new JPanel(new GridLayout(1,2,15,15)); row.setOpaque(false);
        row.add(button("VOTER LOGIN",()->login(false)));
        row.add(button("ADMIN LOGIN",()->login(true)));
        actions.add(text("Choose how you want to continue",18,true),BorderLayout.NORTH);
        actions.add(row,BorderLayout.CENTER); middle.add(stats); middle.add(actions);
        main.add(middle,BorderLayout.CENTER); body(root).add(main,BorderLayout.CENTER);
        display(root);
    }
    void login(boolean admin) {
        JPanel root=layout(admin?"Admin Login":"Voter Login",false), box=card(), f=stack();
        addLine(f,text(admin?"Administrator Access":"Your Vote Matters",21,true));
        JTextField id=field(admin?"Admin ID":"Voter ID"); JPasswordField pw=password();
        addLine(f,id); addLine(f,pw);
        addLine(f,button("LOGIN",()->{
            String i=id.getText().trim(),pass=new String(pw.getPassword());
            if(admin) {
                if(i.equals("admin")&&pass.equals("admin123")) {adminSession=true;dashboard();}
                else message("Invalid admin login");
            } else {
                Voter v=voters.get(i);
                if(v==null||!v.pass.equals(pass))message("Invalid voter ID or password");
                else {current=v;votePage();}
            }
        }));
        addLine(f,button("BACK",this::home)); box.add(f,BorderLayout.CENTER); centered(root,box);
    }
    void dashboard() {
        if(!adminSession){login(true);return;}
        JPanel root=layout("Admin Dashboard",true),main=new JPanel(new BorderLayout(18,18));
        main.setOpaque(false);
        main.add(banner("Welcome, Admin","Manage voters and election results"),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout(18,18));mid.setOpaque(false);
        JPanel stats=new JPanel(new GridLayout(1,3,15,15));stats.setOpaque(false);
        stats.add(stat("Registered Voters",voters.size()));
        stats.add(stat("Votes Cast",votedCount()));
        stats.add(stat("Pending Voters",voters.size()-votedCount()));
        JPanel actions=new JPanel(new GridLayout(2,3,15,15));actions.setOpaque(false);
        actions.add(button("Register Voter",this::register));
        actions.add(button("Import CSV",this::importCSV));
        actions.add(button("View Voters",this::listVoters));
        actions.add(button("Manage Votes",this::manage));
        actions.add(button("Results",this::results));
        actions.add(button("Logout",()->{adminSession=false;home();}));
        mid.add(stats,BorderLayout.NORTH);mid.add(actions,BorderLayout.CENTER);
        main.add(mid,BorderLayout.CENTER);body(root).add(main,BorderLayout.CENTER);display(root);
    }
    void register() {
        if(!adminSession){login(true);return;}
        JPanel root=layout("Voter Registration",true),box=card(),f=stack();
        addLine(f,text("Register a New Voter",21,true));
        JTextField id=field("Voter ID"),name=field("Full Name");JPasswordField pw=password();
        addLine(f,id);addLine(f,name);addLine(f,pw);
        addLine(f,button("REGISTER VOTER",()->{
            String i=id.getText().trim(),n=name.getText().trim(),pass=new String(pw.getPassword());
            if(i.isEmpty()||n.isEmpty()||pass.isEmpty()){message("Fill all fields");return;}
            if(voters.containsKey(i)){message("Voter ID already exists");return;}
            voters.put(i,new Voter(i,n,pass));
            if(!save()){voters.remove(i);return;}
            message("Registration successful!");dashboard();
        }));box.add(f,BorderLayout.CENTER);centered(root,box);
    }
    void importCSV() {
        if(!adminSession){login(true);return;}
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Select Voters CSV File");
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Map<String,Voter> added=new LinkedHashMap<>();int skipped=0;
        try(BufferedReader br=new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
            String line;boolean first=true;
            while((line=br.readLine())!=null) {
                if(line.trim().isEmpty())continue;
                String[] d=line.split(",",-1);
                if(d.length<3){skipped++;continue;}
                String id=d[0].trim(),name=d[1].trim(),pass=d[2].trim();
                if(first&&(id.equalsIgnoreCase("VoterID")||id.equalsIgnoreCase("Voter ID")||
                    id.equalsIgnoreCase("ID"))){first=false;continue;}
                first=false;
                if(id.isEmpty()||name.isEmpty()||pass.isEmpty()||voters.containsKey(id)||
                   added.containsKey(id)){skipped++;continue;}
                added.put(id,new Voter(id,name,pass));
            }
            voters.putAll(added);
            if(!added.isEmpty()&&!save()){
                for(String id:added.keySet())voters.remove(id);return;
            }
            message("Imported: "+added.size()+"\nSkipped: "+skipped+"\nTotal voters: "+voters.size());
            dashboard();
        }catch(IOException e){message("CSV Error: "+e.getMessage());}
    }
    void listVoters() {
        if(!adminSession){login(true);return;}
        JPanel root=layout("Registered Voters",true),box=card();
        DefaultTableModel model=new DefaultTableModel(new String[]{"Voter ID","Name","Voted"},0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        for(Voter v:voters.values())model.addRow(new Object[]{v.id,v.name,v.voted?"Yes":"No"});
        JTable table=new JTable(model);table.setRowHeight(35);
        table.setFont(new Font("Segoe UI",Font.PLAIN,14));
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));
        table.getTableHeader().setBackground(new Color(220,239,244));
        box.add(text("Total Registered Voters: "+voters.size(),18,true),BorderLayout.NORTH);
        box.add(new JScrollPane(table),BorderLayout.CENTER);
        body(root).add(box,BorderLayout.CENTER);display(root);
    }
    void votePage() {
        JPanel root=layout("Cast Your Vote",false),box=card();
        box.add(banner("Welcome, "+current.name,"Choose your preferred candidate"),BorderLayout.NORTH);
        if(current.voted) {
            JPanel f=stack();addLine(f,text("You have already voted!",22,true));
            addLine(f,text("Only one vote is allowed per voter.",16,false));
            addLine(f,button("LOGOUT",()->{current=null;home();}));
            box.add(f,BorderLayout.CENTER);
        }else{
            JPanel choices=new JPanel(new GridLayout(1,3,15,15));choices.setOpaque(false);
            ButtonGroup group=new ButtonGroup();JRadioButton[] radio=new JRadioButton[3];
            for(int i=0;i<3;i++) {
                JPanel c=card();JLabel symbol=text("●",42,true);
                symbol.setHorizontalAlignment(SwingConstants.CENTER);symbol.setForeground(TEAL);
                c.add(symbol,BorderLayout.NORTH);
                radio[i]=new JRadioButton(candidates[i]);radio[i].setOpaque(false);
                radio[i].setFont(new Font("Segoe UI",Font.BOLD,16));
                radio[i].setHorizontalAlignment(SwingConstants.CENTER);
                group.add(radio[i]);c.add(radio[i],BorderLayout.CENTER);choices.add(c);
            }
            radio[0].setSelected(true);box.add(choices,BorderLayout.CENTER);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.CENTER,16,5));actions.setOpaque(false);
            actions.add(button("SUBMIT VOTE",()->{
                int i=0;while(i<2&&!radio[i].isSelected())i++;
                if(JOptionPane.showConfirmDialog(this,"Vote for "+candidates[i]+"?",
                   "Confirm Vote",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                if(current.voted){message("Already voted!");return;}
                votes[i]++;current.voted=true;
                if(!save()){votes[i]--;current.voted=false;return;}
                message("Vote submitted successfully!");current=null;home();
            }));
            actions.add(button("LOGOUT",()->{current=null;home();}));
            box.add(actions,BorderLayout.SOUTH);
        }
        body(root).add(box,BorderLayout.CENTER);display(root);
    }
    void manage() {
        if(!adminSession){login(true);return;}
        JPanel root=layout("Vote Management",true),box=card(),rows=stack();int total=0;
        for(int i=0;i<3;i++){addLine(rows,text(candidates[i]+": "+votes[i]+" votes",19,true));total+=votes[i];}
        addLine(rows,text("Total Votes: "+total,21,true));
        box.add(rows,BorderLayout.CENTER);
        box.add(button("VIEW RESULTS",this::results),BorderLayout.SOUTH);
        body(root).add(box,BorderLayout.CENTER);display(root);
    }
    void results() {
        if(!adminSession){login(true);return;}
        JPanel root=layout("Election Results",true),box=card(),rows=stack();
        int total=0,max=0,winner=-1,ties=0;
        for(int v:votes){total+=v;max=Math.max(max,v);}
        for(int i=0;i<3;i++) {
            addLine(rows,text(candidates[i]+" : "+votes[i]+" votes",17,true));
            JProgressBar bar=new JProgressBar(0,Math.max(total,1));
            bar.setValue(votes[i]);bar.setForeground(TEAL);bar.setStringPainted(true);
            bar.setString(total==0?"0%":(100*votes[i]/total)+"%");
            addLine(rows,bar);
            if(votes[i]==max){ties++;winner=i;}
        }
        addLine(rows,text("Total Votes: "+total,19,true));
        addLine(rows,text("Winner: "+(total==0?"No votes yet":ties>1?"Tie":candidates[winner]),21,true));
        box.add(rows,BorderLayout.CENTER);body(root).add(box,BorderLayout.CENTER);display(root);
    }
    boolean save() {
        try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(FILE))){
            out.writeObject(new LinkedHashMap<>(voters));out.writeObject(votes);return true;
        }catch(IOException e){message("Save failed: "+e.getMessage());return false;}
    }
    @SuppressWarnings("unchecked") void load() {
        File file=new File(FILE);
        if(file.exists()) {
            try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){
                voters=(Map<String,Voter>)in.readObject();votes=(int[])in.readObject();return;
            }catch(Exception e){message("Data loading failed: "+e.getMessage());System.exit(1);}
        }
        voters.put("V101",new Voter("V101","Arun","1234"));
        voters.put("V102",new Voter("V102","Priya","5678"));
        voters.put("V103",new Voter("V103","Kumar","9999"));save();
    }
    public static void main(String[] args){SwingUtilities.invokeLater(OnlineVotingSystem::new);}
}
