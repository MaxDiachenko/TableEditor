import org.koin.dsl.module
import app.*
import app.data.*
import app.expressions.*
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

val appModule = module {
    factory<(Int, Int) -> IDataModel>(named("sparse")) { {rows, cols -> SparseDataModel(rows, cols) }}
    factory<(Int, Int) -> IDataModel>(named("dense")) { {rows, cols -> DenseDataModel(rows, cols) }}
    factory<IDocumentVm> { (data: IDataModel, isSparse: Boolean) -> DocumentVm(
        get(),
        get(qualifier = named("dense")),
        get(qualifier = named("sparse")),
        { m -> get<IEvaluator> { parametersOf(m) } },
        data, isSparse) }
    factory<IEvaluator> { (data: IDataModel) -> Evaluator(data) }
    single<IDataModelReader> { DataModelSerializer }
    single<IDataModelWriter> { DataModelSerializer }
    single<IEditorVm> { EditorVm(get(), get(),  { m, b -> get<IDocumentVm> { parametersOf(m, b) } },
        get(qualifier = named("sparse"))) }
    single<ICellFactory> { CellFactory }
}
