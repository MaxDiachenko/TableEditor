package app.data

import java.io.File

interface IDataModelWriter {
    fun write(model: IDataModel, file: File): Unit
}
