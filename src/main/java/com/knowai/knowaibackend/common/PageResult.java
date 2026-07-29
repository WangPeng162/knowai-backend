package com.knowai.knowaibackend.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PageResult<T> {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页数据
     */
    private List<T> records;

    public PageResult(Long total, List<T> records){
        this.total = total;
        this.records = records;
    }

}