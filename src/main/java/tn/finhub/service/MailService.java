package tn.finhub.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailService {

  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String SMTP_PORT = "587";

  // ⚠️ USE A DEDICATED APP PASSWORD
  private static final String FROM_EMAIL = "sadok.dridi.engineer@gmail.com";
  private static final String APP_PASSWORD = "lzmtibuiovhabntn";

  private static Session createSession() {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", SMTP_HOST);
    props.put("mail.smtp.port", SMTP_PORT);
    props.put("mail.smtp.starttls.required", "true");

    return Session.getInstance(
        props,
        new Authenticator() {
          @Override
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
          }
        });
  }

  // ===============================
  // USER EMAIL VERIFICATION
  // ===============================
  public static void sendVerificationEmail(String to, String link) {
    sendHtmlEmail(
        to,
        "Verify your FINHUB email address",
        buildVerificationHtml(link));
  }

  // ===============================
  // ADMIN INVITATION EMAIL ✅ NEW
  // ===============================
  public static void sendAdminInviteEmail(String to, String link) {
    sendHtmlEmail(
        to,
        "You have been invited as a FINHUB Administrator",
        buildAdminInviteHtml(link));
  }

  // ===============================
  // OTP EMAIL ✅ NEW
  // ===============================
  public static void sendOtpEmail(String to, String otp) {
    sendHtmlEmail(
        to,
        "FINHUB Transaction Verification Code",
        buildOtpHtml(otp));
  }

  // ===============================
  // CORE EMAIL SENDER
  // ===============================
  private static void sendHtmlEmail(String to, String subject, String html) {
    try {
      Session session = createSession();

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(FROM_EMAIL, "FINHUB Security"));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
      message.setSubject(subject);
      message.setContent(html, "text/html; charset=UTF-8");

      Transport.send(message);
      System.out.println("✅ Email sent to " + to);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Email sending failed", e);
    }
  }

  // ===============================
  // EMAIL TEMPLATES
  // ===============================
  private static String buildVerificationHtml(String link) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Verify your FINHUB account</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">

        <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:40px 0;">
          <tr>
            <td align="center">

              <table width="520" cellpadding="0" cellspacing="0"
                     style="background-color:#ffffff; border-radius:12px; padding:32px;
                            box-shadow:0 6px 20px rgba(0,0,0,0.08);">

                <!-- Header -->
                <tr>
                  <td align="center" style="padding-bottom:24px;">
                    <h1 style="margin:0; color:#111827; font-size:26px;">FINHUB</h1>
                    <p style="margin:6px 0 0; color:#6b7280; font-size:14px;">
                      Secure Financial Platform
                    </p>
                  </td>
                </tr>

                <!-- Content -->
                <tr>
                  <td style="color:#111827; font-size:15px; line-height:1.7;">
                    <p>Hello,</p>

                    <p>
                      Thank you for creating a FINHUB account.
                      Please confirm your email address to activate your account.
                    </p>

                    <!-- Button -->
                    <p style="text-align:center; margin:32px 0;">
                      <a href="{{LINK}}"
                         style="
                           display:inline-block;
                           padding:14px 30px;
                           background-color:#2563eb;
                           color:#ffffff;
                           text-decoration:none;
                           font-weight:700;
                           border-radius:8px;
                           font-size:15px;
                         ">
                        Verify Email Address
                      </a>
                    </p>

                    <p style="font-size:14px;">
                      If the button doesn’t work, copy and paste this link into your browser:
                    </p>

                    <p style="word-break:break-all; font-size:13px; color:#2563eb;">
                      {{LINK}}
                    </p>

                    <p style="margin-top:24px;">
                      This verification link will expire in <strong>24 hours</strong>.
                    </p>

                    <p style="font-size:14px; color:#6b7280;">
                      If you did not create this account, you can safely ignore this email.
                    </p>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding-top:24px; border-top:1px solid #e5e7eb;
                             text-align:center; font-size:12px; color:#6b7280;">
                    © {{YEAR}} FINHUB. All rights reserved.
                  </td>
                </tr>

              </table>

            </td>
          </tr>
        </table>

        </body>
        </html>
        """
        .replace("{{LINK}}", link)
        .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
  }

  private static String buildAdminInviteHtml(String link) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>FINHUB Admin Invitation</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">

        <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:40px 0;">
          <tr>
            <td align="center">

              <table width="520" cellpadding="0" cellspacing="0"
                     style="background-color:#ffffff; border-radius:12px; padding:32px;
                            box-shadow:0 6px 20px rgba(0,0,0,0.08);">

                <!-- Header -->
                <tr>
                  <td align="center" style="padding-bottom:24px;">
                    <h1 style="margin:0; color:#111827; font-size:26px;">FINHUB</h1>
                    <p style="margin:6px 0 0; color:#6b7280; font-size:14px;">
                      Secure Financial Platform
                    </p>
                  </td>
                </tr>

                <!-- Content -->
                <tr>
                  <td style="color:#111827; font-size:15px; line-height:1.7;">
                    <p>Hello,</p>

                    <p>
                      You have been invited to become an <strong>Administrator</strong> on FINHUB.
                      This role grants access to administrative and management features.
                    </p>

                    <!-- Button -->
                    <p style="text-align:center; margin:32px 0;">
                      <a href="{{LINK}}"
                         style="
                           display:inline-block;
                           padding:14px 30px;
                           background-color:#2563eb;
                           color:#ffffff;
                           text-decoration:none;
                           font-weight:700;
                           border-radius:8px;
                           font-size:15px;
                         ">
                        Accept Admin Invitation
                      </a>
                    </p>

                    <p style="font-size:14px;">
                      If the button doesn’t work, copy and paste this link into your browser:
                    </p>

                    <p style="word-break:break-all; font-size:13px; color:#2563eb;">
                      {{LINK}}
                    </p>

                    <p style="margin-top:24px;">
                      This verification link will expire in <strong>24 hours</strong>.
                    </p>

                    <p style="font-size:14px; color:#6b7280;">
                      If you did not create this account, you can safely ignore this email.
                    </p>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding-top:24px; border-top:1px solid #e5e7eb;
                             text-align:center; font-size:12px; color:#6b7280;">
                    © {{YEAR}} FINHUB. All rights reserved.
                  </td>
                </tr>

              </table>

            </td>
          </tr>
        </table>

        </body>
        </html>
        """
        .replace("{{LINK}}", link)
        .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
  }

  private static String buildOtpHtml(String otp) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Transaction Verification</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">

        <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:40px 0;">
          <tr>
            <td align="center">

              <table width="520" cellpadding="0" cellspacing="0"
                     style="background-color:#ffffff; border-radius:12px; padding:32px;
                            box-shadow:0 6px 20px rgba(0,0,0,0.08);">

                <!-- Header -->
                <tr>
                  <td align="center" style="padding-bottom:24px;">
                    <h1 style="margin:0; color:#111827; font-size:26px;">FINHUB</h1>
                    <p style="margin:6px 0 0; color:#6b7280; font-size:14px;">
                      Secure Transaction
                    </p>
                  </td>
                </tr>

                <!-- Content -->
                <tr>
                  <td style="color:#111827; font-size:15px; line-height:1.7;">
                    <p>Hello,</p>

                    <p>
                      To complete your transaction, please enter the following verification code:
                    </p>

                    <!-- Code -->
                    <p style="text-align:center; margin:32px 0;">
                      <span style="
                           display:inline-block;
                           padding:14px 40px;
                           background-color:#f3f4f6;
                           color:#111827;
                           font-family:'Courier New', monospace;
                           font-weight:700;
                           font-size:32px;
                           letter-spacing: 5px;
                           border-radius:8px;
                           border: 1px solid #e5e7eb;
                         ">
                        {{OTP}}
                      </span>
                    </p>

                    <p style="text-align:center; font-size:14px; color:#ef4444;">
                      Do not share this code with anyone.
                    </p>

                    <p style="margin-top:24px; font-size:14px; color:#6b7280;">
                      If you did not initiate this transaction, please contact support immediately.
                    </p>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding-top:24px; border-top:1px solid #e5e7eb;
                             text-align:center; font-size:12px; color:#6b7280;">
                    © {{YEAR}} FINHUB. All rights reserved.
                  </td>
                </tr>

              </table>

            </td>
          </tr>
        </table>

        </body>
        </html>
        """
        .replace("{{OTP}}", otp)
        .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
  }

  // ===============================
  // RESET PASSWORD EMAIL ✅ NEW
  // ===============================
  public static void sendResetPasswordEmail(String to, String link) {
    sendHtmlEmail(
        to,
        "Reset your FINHUB password",
        buildResetPasswordHtml(link));
  }

  private static String buildResetPasswordHtml(String link) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Reset your FINHUB password</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">

        <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:40px 0;">
          <tr>
            <td align="center">

              <table width="520" cellpadding="0" cellspacing="0"
                     style="background-color:#ffffff; border-radius:12px; padding:32px;
                            box-shadow:0 6px 20px rgba(0,0,0,0.08);">

                <!-- Header -->
                <tr>
                  <td align="center" style="padding-bottom:24px;">
                    <h1 style="margin:0; color:#111827; font-size:26px;">FINHUB</h1>
                    <p style="margin:6px 0 0; color:#6b7280; font-size:14px;">
                      Secure Financial Platform
                    </p>
                  </td>
                </tr>

                <!-- Content -->
                <tr>
                  <td style="color:#111827; font-size:15px; line-height:1.7;">
                    <p>Hello,</p>

                    <p>
                      We received a request to reset the password for your FINHUB account.
                      Click the button below to proceed.
                    </p>

                    <!-- Button -->
                    <p style="text-align:center; margin:32px 0;">
                      <a href="{{LINK}}"
                         style="
                           display:inline-block;
                           padding:14px 30px;
                           background-color:#2563eb;
                           color:#ffffff;
                           text-decoration:none;
                           font-weight:700;
                           border-radius:8px;
                           font-size:15px;
                         ">
                        Reset Password
                      </a>
                    </p>

                    <p style="font-size:14px;">
                      If the button doesn’t work, copy and paste this link into your browser:
                    </p>

                    <p style="word-break:break-all; font-size:13px; color:#2563eb;">
                      {{LINK}}
                    </p>

                    <p style="margin-top:24px;">
                      This link will expire in <strong>24 hours</strong>.
                    </p>

                    <p style="font-size:14px; color:#6b7280;">
                      If you did not request a password reset, you can safely ignore this email.
                    </p>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding-top:24px; border-top:1px solid #e5e7eb;
                             text-align:center; font-size:12px; color:#6b7280;">
                    © {{YEAR}} FINHUB. All rights reserved.
                  </td>
                </tr>

              </table>

            </td>
          </tr>
        </table>

        </body>
        </html>
        """
        .replace("{{LINK}}", link)
        .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
  }
}
