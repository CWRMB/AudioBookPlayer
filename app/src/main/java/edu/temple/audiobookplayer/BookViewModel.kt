package edu.temple.audiobookplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BookViewModel : ViewModel() {
    val selectedBook: MutableLiveData<Book> by lazy{
        MutableLiveData<Book>()
    }

    fun setSelectedBook(book: Book){
        selectedBook.value = book
    }

    fun getSelectedBook() : LiveData<Book>{
        // return mutable live data object from inheritance
        return selectedBook
    }

}