package edu.temple.audiobookplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val BOOKS_KEY = "book_key"

/**
 * A simple [Fragment] subclass.
 * Use the [BookListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookListFragment : Fragment() {
    private var books: Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            books = it.getStringArray(BOOKS_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_book_list, container, false) as RecyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view as RecyclerView){
            // define attributes of view object which are defined in this scope as a recyclerView
            // requireContext() to tell it I am insisting there is an attached context
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    class BookAdapter(_books: Array<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>(){
        val books = _books

        class BookViewHolder(_view: View) : RecyclerView.ViewHolder(_view){
            val view = _view

            // section for onclick listener to update our clicked object
            init{

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            // Have to change the xml layout to a recycler view
            val layout = LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_book_list, parent, false
            )

            return BookViewHolder(layout)
        }

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            TODO("Not yet implemented")
        }

        override fun getItemCount(): Int {
            return books.size
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(book_list: BookList) =
            BookListFragment().apply {
                arguments = Bundle().apply {
                    // use Serializable to pass a byte stream since our book_list is an object array
                    // of book
                    putSerializable(BOOKS_KEY, book_list.books)
                }
            }
    }
}