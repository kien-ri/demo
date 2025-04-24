package com.kien.blog.repository

import com.kien.blog.model.condition.BookCondition
import com.kien.blog.model.view.BookView
import org.apache.ibatis.annotations.Mapper

@Mapper
interface BookMapper {

    fun getById(id: Long): BookView?

    fun getListByCondition(bookCondition: BookCondition): List<BookView>

    fun getCountByCondition(bookCondition: BookCondition): Int
}