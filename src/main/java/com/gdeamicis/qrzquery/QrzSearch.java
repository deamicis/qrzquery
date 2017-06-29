package com.gdeamicis.qrzquery;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import org.apache.http.ParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class QrzSearch {

    //Swing components
    private static JFrame frame;
    private static javax.swing.JPanel inputPanel;
    private static javax.swing.JTextField inputTextField;
    private static javax.swing.JPanel outputPanel;
    private static javax.swing.JTextArea outputTextArea;
    private static javax.swing.border.TitledBorder inputPanelLabel;
    private static javax.swing.border.TitledBorder outputPanelLabel;

    private static HttpUrlConnection httpclient;

    //variables
    private static String userAgent;
    private static String qrzSite;
    private static String loginUrl;
    private static String qrzUsername;
    private static String qrzPassword;
    private static String qrzQuerypost;
    private static Boolean loggedIn;

    //output variables
    private static String rCallsign;
    private static String rNazione;
    private static String rInfocontatto;
    private static String rLookUp;

    private static String output;
    private static String page;

    private static void doWebcall(java.awt.event.ActionEvent evt, String callsign){
        outputTextArea.setText(callsign);
        //doSpeak(callsign);
        if (!loggedIn){
            doLogin();
        }
        doSearch();
        outputTextArea.setFocusable(true);
        inputTextField.setText("");
    }
    private static void doSearch(){
        // We should be already logged in then query for contact.
        String result;
        try {
            result = httpclient.GetPageContent(qrzQuerypost+"/"+inputTextField.getText(), userAgent);
            doParsing(result);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void doLogin(){
        try {
            httpclient = new HttpUrlConnection();

            // 1. Send a "GET" request, so that you can extract the form's data.
            page = httpclient.GetPageContent(loginUrl, userAgent);
            String postParams = httpclient.getFormParams(page, qrzUsername, qrzPassword);
            // 2. Construct above post's content and then send a POST request for authentication
            httpclient.sendPost(loginUrl, postParams);
            loggedIn = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void doParsing (String htmlResult){
        try {
            Document document = Jsoup.parse(htmlResult, qrzSite);
            if (document.getElementById("csdata") != null ){
                //found

                //System.out.println("DEBUG INFO\n" + document.getElementById("csdata"));
                rCallsign = document.getElementById("csdata").select("[class^=csign]").text();
                document.getElementById("csdata").select("[class^=csign]").remove();
                //rNazione = document.getElementById("csdata").select("a[href]").get(1).text();

                rNazione = document.getElementById("csdata").select("span").get(1).text();
                //System.out.println("NAZIONE: " + rNazione+"\n");

                document.getElementById("csdata").select("span").get(1).remove();
                //document.getElementById("csdata").select("span").get(2).remove();
                document.getElementById("csdata").select("[class^=csgnl]").remove();
                //System.out.println(document.getElementById("csdata"));
                //rInfocontatto = document.getElementById("csdata").select("p").text();
                rInfocontatto = document.getElementById("csdata").select("[class=m0]").text();
                //String out = "";
                rLookUp = document.getElementById("csdata").select("[class=ml1]").text();
                //grab button
                //	System.out.println(document.getElementsByTag("button").attr("onclick").text() );
                //System.out.println("CALLSIGN: " +rCallsign+"\n");
                //System.out.println("NAZIONE: " + rNazione+"\n");
                //System.out.println("INFO CONTATTO: " + rInfocontatto+"\n");
                //out = "CALLSIGN: " +rCallsign+"\n" + "NAZIONE: " + rNAzione+"\n" + "INFO CONTATTO: " + rInfocontatto+"\n";

                String out = doOutput();
                //System.out.println(out);
                outputTextArea.setText(out);
            }
            else
                outputTextArea.setText(inputTextField.getText() + " non trovato!!\n");
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // create custom output if available
    //Available var:  _callsign_ _nazione_ _info_
    private static String doOutput(){
        String tmp;
        if (emptyField(output)){
            tmp = "CALLSIGN: " +rCallsign+"\n" + "NAZIONE: " + rNazione+ "\n" + "INFO: " + rInfocontatto+"\n" + "Lookups:" + rLookUp ;
        }
        else {
            tmp = output.replace("_callsign_", rCallsign);
            tmp = tmp.replace("_nazione_", rNazione);
            tmp = tmp.replace("_info_", rInfocontatto);
            tmp = tmp.replace("_lookup_", rLookUp);
        }
        return tmp;
    }

    //info value for do the job
    private static void getProperties(){
        Properties prop = new Properties();
        try {

            prop.load(new FileInputStream("config.properties"));

            //Works if resource folder is parent of java folder..
            //InputStream inputStream = QrzSearch.class.getClassLoader().getResourceAsStream("config.properties");
            //prop.load(inputStream);

            userAgent = prop.getProperty("user_agent");
            qrzSite = prop.getProperty("qrz_site");
            loginUrl = prop.getProperty("login_url");
            qrzUsername = prop.getProperty("qrz_username");
            qrzPassword = prop.getProperty("qrz_password");
            qrzQuerypost = prop.getProperty("qrz_query_post");
            output = prop.getProperty("output_format");
            if (emptyField(userAgent) ||  emptyField(qrzSite) || emptyField(loginUrl) ||
                    emptyField(qrzUsername) || emptyField(qrzPassword) || emptyField(qrzQuerypost)){
                outputTextArea.setText("Property File incompleto QUIT!!!");
                //doNothing = true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static boolean emptyField(String field){
        Boolean ret = false;
        if (field == null || field.length() == 0)
            ret = true;
        return ret;
    }

    //Close and clean all
    private static void onExit(){
        //ALL method on close windows
        loggedIn = false;
        if (httpclient != null)
            httpclient.closeConnection();
        frame.dispose();
        System.exit(0);
    }

    public static void addComponentsToPane(Container pane) {
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        inputTextField = new javax.swing.JTextField();
        inputTextField.setPreferredSize(new Dimension(350,50));
        inputTextField.setFont(new Font("", Font.BOLD,20));
        //inputTextField.getAccessibleContext().setAccessibleName("Callsign");
        //inputTextField.getAccessibleContext().setAccessibleDescription("Inserire qui il Callsign da cercare");
        inputTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputTextField.requestFocus();
        inputTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doWebcall(evt,inputTextField.getText());
            }
        });

        outputTextArea = new javax.swing.JTextArea();
        outputTextArea.setPreferredSize(new Dimension(350,150));
        //outputTextArea.getAccessibleContext().setAccessibleName("Risultato");
        //outputTextArea.getAccessibleContext().setAccessibleDescription("Sezione Risultato");
        outputTextArea.setEditable(false);
        outputTextArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        inputPanelLabel = new javax.swing.border.TitledBorder("Callsign: ");

        outputPanelLabel = new javax.swing.border.TitledBorder("Risultato: ");

        inputPanel =  new javax.swing.JPanel();
        outputPanel =  new javax.swing.JPanel();

        //inputPanel.getAccessibleContext().setAccessibleName("Input");
        //inputPanel.getAccessibleContext().setAccessibleDescription("Tab Callsign");
        //outputPanel.getAccessibleContext().setAccessibleName("Output");
        //outputPanel.getAccessibleContext().setAccessibleDescription("Tab Risultato");

        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.setBorder(inputPanelLabel);

        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.X_AXIS));
        outputPanel.setBorder(outputPanelLabel);

        inputPanel.add(inputTextField);
        outputPanel.add(outputTextArea);
        pane.add(inputPanel);
        pane.add(outputPanel);
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        loggedIn = false;
        //Create and set up the window.
        frame = new JFrame("QRZ Query");
        //frame.getAccessibleContext().setAccessibleName("Query QRZ");
        //frame.getAccessibleContext().setAccessibleDescription("Finestra Principale");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                onExit();
            }
        });
        //frame.setMinimumSize(new java.awt.Dimension(350,200));
        //Set up the content pane.
        addComponentsToPane(frame.getContentPane());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        getProperties();
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ITALIAN);
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
