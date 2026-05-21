package com.example.EDIP.document.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ReplyRequest {


    private String replyText;


    private MultipartFile replyFile;


}