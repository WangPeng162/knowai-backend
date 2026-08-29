package com.knowai.knowaibackend.domain.document;

import lombok.Data;

@Data
public class DocumentPage {

    /**
     * 页码
     */
    private Integer pageNumber;


    /**
     * 页面文本内容
     */
    private String content;

}
