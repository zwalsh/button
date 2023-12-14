package sh.zachwal.button.wrapped

import javax.inject.Inject


class WrappedService @Inject constructor(

) {

    fun wrapped(year: Int, id: String): Wrapped {
        return Wrapped(
            year = year,
            id = id
        )
    }
}
