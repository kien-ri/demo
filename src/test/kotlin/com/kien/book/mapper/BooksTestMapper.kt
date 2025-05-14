package com.kien.book.mapper

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select

@Mapper
interface BooksTestMapper {
    @Select("SELECT MAX(id) FROM books")
    fun getMaxId(): Long?
}
