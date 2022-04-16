package edu.temple.audiobookplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val BOOKS_KEY = "book_key"

/**
 * A simple [Fragment] subclass.
 * Use the [BookListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookListFragment(): Fragment() {
    private var books: ArrayList<Book>? = null
    private lateinit var bookViewModel: BookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookViewModel = ViewModelProvider(requireActivity()).get(BookViewModel::class.java)

        arguments?.let {
            // get the arraylist from the key in newInstance()
            @Suppress("UNCHECKED_CAST")
            books = it.getSerializable(BOOKS_KEY) as ArrayList<Book>?
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
            books?.run{

                // on click function for each book element
                val clickEvent = {
                    book: Book -> bookViewModel.setSelectedBook(book)
                    (requireActivity() as BookFragmentInterface).bookSelected()
                }

                // define attributes of view object which are defined in this scope as a recyclerView
                // requireContext() to tell it I am insisting there is an attached context
                layoutManager = LinearLayoutManager(requireContext())
                adapter = BookAdapter(BookList(this), clickEvent)
            }
        }
    }

    // out adapter class for our recycler
    class BookAdapter(_books: BookList, _clickEvent: (Book) -> Unit) : RecyclerView.Adapter<BookAdapter.BookViewHolder>(){
        val books = _books
        val clickEvent = _clickEvent

        class BookViewHolder(_view: View) : RecyclerView.ViewHolder(_view){
            val view = _view
            val author = view.findViewById<TextView>(R.id.display_author)
            val title = view.findViewById<TextView>(R.id.text_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            // inflate out xml layout that holds the text attributes for each book in recycler
            val layout = LayoutInflater.from(parent.context).inflate(
                R.layout.book_list_layout, parent, false
            )

            return BookViewHolder(layout)
        }

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            // books.books meaning our books list accessing the data field that is the string called books
            // next time better naming would be good
            holder.author.text = books.books[position].author
            holder.title.text = books.books[position].title


            // on click listener for what index we press on
            holder.view.setOnClickListener{ clickEvent(books.books[position]) }
        }

        override fun getItemCount(): Int {
            return books.books.size
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

    public interface BookFragmentInterface {
        fun bookSelected()
    }
}