package app.data

import java.io.File

interface IDataModelReader {
    fun read(file: File, factory: (rows: Int, cols: Int) -> IDataModel): IDataModel
}
