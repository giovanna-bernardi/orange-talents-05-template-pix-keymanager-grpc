package br.com.zupacademy.giovanna.compartilhado

import org.jasypt.util.text.BasicTextEncryptor
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class SensitiveDataKeyValueConverter: AttributeConverter<String, String> {

    private val textEncryptor: BasicTextEncryptor = BasicTextEncryptor()
    init{
        textEncryptor.setPassword("\${keyvalue.secret}")
    }

    override fun convertToDatabaseColumn(attribute: String?): String {
        return textEncryptor.encrypt(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String {
        return textEncryptor.decrypt(dbData)
    }
}