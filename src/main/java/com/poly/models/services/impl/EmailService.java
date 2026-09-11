package com.poly.models.services.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.poly.models.entities.Order;
import com.poly.models.entities.OrderDetail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Async
    public void sendEmailWithInlineImage(String to, Order order) throws Exception { //asynchronous with js
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("nv80804@gmail.com");
        helper.setTo(to);
        helper.setSubject("Thông tin đơn hàng");

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Build order details rows
        StringBuilder rows = new StringBuilder();
        int index = 1;

        for (OrderDetail od : order.getOrderDetails()) {
            rows.append("<tr>")
                .append("<td style='border:1px solid #ddd;padding:8px;text-align:center;'>").append(index++).append("</td>")
                .append("<td style='border:1px solid #ddd;padding:8px;text-align:left;'>").append(od.getName()).append("</td>")
                .append("<td style='border:1px solid #ddd;padding:8px;text-align:center;'>")
                .append(formatter.format(od.getProduct().getPrice())).append(" VND</td>")
                .append("<td style='border:1px solid #ddd;padding:8px;text-align:center;'>")
                .append(od.getQuantity()).append("</td>")
                .append("<td style='border:1px solid #ddd;padding:8px;text-align:center;'>")
                .append(formatter.format(od.getProduct().getPrice().multiply(new BigDecimal(od.getQuantity())))).append(" VND</td>")
                .append("</tr>");
        }

        String htmlContent = """
        <div style="font-family:Arial, sans-serif; max-width:700px; margin:auto; border:1px solid #ddd;">
            
            <!-- Header -->
            <div style="background:#212529; color:white; padding:10px; text-align:center;">
                <img src="cid:logoImage" width="120"/><br/>
                <h2 style="margin:5px 0;">Thông tin đơn hàng</h2>
            </div>

            <!-- Customer Info -->
            <div style="padding:15px;">
                <h3 style="margin-bottom:10px;">Thông tin khách hàng</h3>
                <p><b>Họ tên:</b> %s</p>
                <p><b>Tài khoản:</b> %s</p>
                <p><b>Địa chỉ:</b> %s</p>
                <p><b>Điện thoại:</b> %s</p>
            </div>

            <!-- Order Table -->
            <div style="padding:15px;">
                <table style="width:100%%; border-collapse:collapse;">
                    <thead>
                        <tr style="background:#212529; color:white;">
                            <th style="padding:8px;border:1px solid #ddd;">#</th>
                            <th style="padding:8px;border:1px solid #ddd;">Tên sản phẩm</th>
                            <th style="padding:8px;border:1px solid #ddd;">Đơn giá</th>
                            <th style="padding:8px;border:1px solid #ddd;">Số lượng</th>
                            <th style="padding:8px;border:1px solid #ddd;">Tổng phụ</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                        <tr style="background:#f2f2f2; font-weight:bold;">
                            <td colspan="4" style="padding:8px;border:1px solid #ddd; text-align:right;">
                                Tổng tiền:
                            </td>
                            <td style="padding:8px;border:1px solid #ddd; color:red; text-align:center;">
                                %s VND
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        """.formatted(
                order.getAccount().getFullname(),
                order.getAccount().getUsername(),
                order.getAccount().getAddress(),
                order.getAccount().getPhone(),
                rows.toString(),
                formatter.format(order.getTotal())
        );

        helper.setText(htmlContent, true);

        // Inline image
        ClassPathResource image = new ClassPathResource("static/images/people_in_1801_bc.jpg");
        helper.addInline("logoImage", image);

        mailSender.send(message);
    }
}
