package com.voiweb.utils.mail;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import jetbrick.template.JetEngine;
import jetbrick.template.JetTemplate;
import jetbrick.template.resolver.GlobalResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/** 
* @author voiweb
* 时间：2017年12月10日
* 类说明 
*/

public class VoiMail {

    private static Session     session;
    private static MimeMessage msg;
    private static String      user;

    private String text;
    private String html;
    private List<MimeBodyPart> attachments = new ArrayList<MimeBodyPart>();

    private VoiMail() {
    }

    public static Properties defaultConfig(Boolean debug) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "false");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.debug", null != debug ? debug.toString() : "false");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.port", "465");
        return props;
    }

    /**
     * smtp aliyun
     *
     * @param debug enable debug
     * @return
     */
    public static Properties SMTP_Aliyun(boolean debug) {
        Properties props = defaultConfig(debug);
        props.put("mail.smtp.host", "smtp.mxhichina.com");
        return props;
    }

    /**
     * smtp qq
     *
     * @param debug enable debug
     * @return
     */
    public static Properties SMTP_QQ(boolean debug) {
        Properties props = defaultConfig(debug);
        props.put("mail.smtp.host", "smtp.qq.com");
        return props;
    }

    /**
     * smtp 163
     *
     * @param debug enable debug
     * @return
     */
    public static Properties SMTP_163(Boolean debug) {
        Properties props = defaultConfig(debug);
        props.put("mail.smtp.host", "smtp.163.com");
        return props;
    }

    /**
     * config username and password
     *
     * @param props    email property config
     * @param username email auth username
     * @param password email auth password
     */
    public static void config(Properties props, final String username, final String password) {
        props.setProperty("username", username);
        props.setProperty("password", password);
        config(props);
    }

    public static void config(Properties props) {
        final String username = props.getProperty("username");
        final String password = props.getProperty("password");
        user = username;
        session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        msg = new MimeMessage(session);
    }

    /**
     * set email subject
     *
     * @param subject subject title
     * @return
     * @throws MessagingException
     */
    public static VoiMail subject(String subject) throws MessagingException {
        VoiMail voiMail = new VoiMail();
        msg.setSubject(subject, "UTF-8");
        return voiMail;
    }

    /**
     * set email from
     *
     * @param nickName from nickname
     * @return
     * @throws MessagingException
     */
    public VoiMail from(String nickName) throws MessagingException {
        return from(nickName, user);
    }

    /**
     * set email nickname and from user
     *
     * @param nickName
     * @param from
     * @return
     * @throws MessagingException
     */
    public VoiMail from(String nickName, String from) throws MessagingException {
        try {
            nickName = MimeUtility.encodeText(nickName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        msg.setFrom(new InternetAddress(nickName + " <" + from + ">"));
        return this;
    }

    public VoiMail replyTo(String... replyTo) throws MessagingException {
        String result = Arrays.asList(replyTo).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", ",");
        msg.setReplyTo(InternetAddress.parse(result));
        return this;
    }

    public VoiMail replyTo(String replyTo) throws MessagingException {
        msg.setReplyTo(InternetAddress.parse(replyTo.replace(";", ",")));
        return this;
    }

    public VoiMail to(String... to) throws Exception {
        return addRecipients(to, Message.RecipientType.TO);
    }

    public VoiMail to(String to) throws MessagingException {
        return addRecipient(to, Message.RecipientType.TO);
    }

    public VoiMail cc(String... cc) throws MessagingException {
        return addRecipients(cc, Message.RecipientType.CC);
    }

    public VoiMail cc(String cc) throws MessagingException {
        return addRecipient(cc, Message.RecipientType.CC);
    }

    public VoiMail bcc(String... bcc) throws MessagingException {
        return addRecipients(bcc, Message.RecipientType.BCC);
    }

    public VoiMail bcc(String bcc) throws MessagingException {
        return addRecipient(bcc, Message.RecipientType.BCC);
    }

    private VoiMail addRecipients(String[] recipients, Message.RecipientType type) throws MessagingException {
        String result = Arrays.asList(recipients).toString().replace("(^\\[|\\]$)", "").replace(", ", ",");
        msg.addRecipients(type, InternetAddress.parse(result));
        return this;
    }

    private VoiMail addRecipient(String recipient, Message.RecipientType type) throws MessagingException {
        msg.addRecipients(type, InternetAddress.parse(recipient.replace(";", ",")));
        return this;
    }

    public VoiMail text(String text) {
        this.text = text;
        return this;
    }

    public VoiMail html(String html) {
        this.html = html;
        return this;
    }

    public VoiMail attach(File file) throws MessagingException {
        attachments.add(createAttachment(file, null));
        return this;
    }
    
    public VoiMail attach(File file, String fileName) throws MessagingException {
    		attachments.add(createAttachment(file, fileName));
    		return this;
    }

    public VoiMail innerAttach(File file , String contentID) throws MessagingException {
        attachments.add(createInnerAttachment(file,contentID));
        return this;
    }

    private MimeBodyPart createAttachment(File file, String fileName) throws MessagingException {
        MimeBodyPart   attachmentPart = new MimeBodyPart();
        FileDataSource fds            = new FileDataSource(file);
        attachmentPart.setDataHandler(new DataHandler(fds));
        try {
            attachmentPart.setFileName(null == fileName ? MimeUtility.encodeText(fds.getName()) : MimeUtility.encodeText(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attachmentPart;
    }
    
    private MimeBodyPart createInnerAttachment(File file, String contentID) throws MessagingException {
        MimeBodyPart   attachmentPart = new MimeBodyPart();
        FileDataSource fds            = new FileDataSource(file);
        attachmentPart.setDataHandler(new DataHandler(fds));
        attachmentPart.setContentID(contentID);
        return attachmentPart;
    }
	
    public static String innerContent(String templatePath , Map<String,Object> map) throws IOException, MessagingException {
	    JetEngine engine = JetEngine.create();
	    GlobalResolver resolver = engine.getGlobalResolver();
	    resolver.registerFunctions(com.voiweb.utils.commons.DateConverter.class);
	    JetTemplate template = engine.getTemplate(templatePath);
	    Map<String, Object> context = map;
	    StringWriter writer = new StringWriter();
	    template.render(context, writer);
	    return writer.toString();
	}
	
    public void send() throws MessagingException {
        if (text == null && html == null)
            throw new NullPointerException("At least one context has to be provided: Text or Html");

        MimeMultipart cover;
        boolean       usingAlternative = false;
        boolean       hasAttachments   = attachments.size() > 0;

        if (text != null && html == null) {
            // TEXT ONLY
            cover = new MimeMultipart("mixed");
            cover.addBodyPart(textPart());
        } else if (text == null && html != null) {
            // HTML ONLY
            cover = new MimeMultipart("mixed");
            cover.addBodyPart(htmlPart());
        } else {
            // HTML + TEXT
            cover = new MimeMultipart("alternative");
            cover.addBodyPart(textPart());
            cover.addBodyPart(htmlPart());
            usingAlternative = true;
        }

        MimeMultipart content = cover;
        if (usingAlternative && hasAttachments) {
            content = new MimeMultipart("mixed");
            content.addBodyPart(toBodyPart(cover));
        }
        for (MimeBodyPart attachment : attachments) {
            content.addBodyPart(attachment);
        }

        msg.setContent(content);
        msg.setSentDate(new Date());
        Transport.send(msg);
    }

    private MimeBodyPart toBodyPart(MimeMultipart cover) throws MessagingException {
        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(cover);
        return wrap;
    }

    private MimeBodyPart textPart() throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(text);
        return bodyPart;
    }

    private MimeBodyPart htmlPart() throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(html, "text/html; charset=utf-8");
        return bodyPart;
    }
}