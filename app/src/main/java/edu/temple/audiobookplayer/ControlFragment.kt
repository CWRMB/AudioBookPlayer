package edu.temple.audiobookplayer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider

class ControlFragment : Fragment(){

    lateinit var bookViewModel: BookViewModel
    lateinit var playButton: Button
    lateinit var pauseButton: Button
    lateinit var stopButton: Button
    lateinit var bookTitle: TextView
    lateinit var seekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookViewModel = ViewModelProvider(requireActivity()).get(BookViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set on click listener for each button
        playButton = view.findViewById(R.id.button_play)
        pauseButton = view.findViewById(R.id.button_pause)
        stopButton = view.findViewById(R.id.button_stop)
        bookTitle = view.findViewById(R.id.titleDisplay)
        seekBar = view.findViewById(R.id.bookProgress)

        seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seek: SeekBar) {

            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                (requireActivity() as ControlFragment.ControlFragmentInterface).progress(seek.progress)
                bookViewModel.setBookProgress(seek.progress)
            }
        })

        var isPaused = false

        // functionality that passes the button presses to our interface to do service interaction with
        // these also change the title of the text view to display the playing title
        playButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).play()
            if(bookViewModel.selectedBook.value != null){
                bookTitle.text = bookViewModel.selectedBook.value?.title.plus(" (Now Playing)")
            }
//            bookViewModel.getBookProgress().observe(requireActivity()){
//                bookViewModel.selectedBook.value?.let{ seekBar.max = it.duration}
//                seekBar.progress = it
//                Log.v("Control prgoress",it.toString())
//            }
        }

        pauseButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).pause()
            if(bookViewModel.selectedBook.value != null && !isPaused){
                bookTitle.text = bookViewModel.selectedBook.value?.title.plus(" (Paused)")
                isPaused = true
            }
            else if(bookViewModel.selectedBook.value != null && isPaused){
                bookTitle.text = bookViewModel.selectedBook.value?.title.plus(" (Now Playing)")
            }
        }

        stopButton.setOnClickListener {
            (requireActivity() as ControlFragment.ControlFragmentInterface).stop()
            bookTitle.text = ""
        }

    }

    // interface to hold abstract data to be implemented into main
    public interface ControlFragmentInterface {
        fun play()
        fun pause()
        fun stop()
        fun progress(progress: Int)
    }
}