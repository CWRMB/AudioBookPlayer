package edu.temple.audiobookplayer

import android.app.Service
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import edu.temple.audlibplayer.PlayerService

class ControlFragment : Fragment(){

    lateinit var bookViewModel: BookViewModel
    lateinit var playButton: Button
    lateinit var pauseButton: Button
    lateinit var stopButton: Button
    lateinit var bookTitle: TextView

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

        playButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).play()
            bookTitle.text = bookViewModel.selectedBook.value?.title.plus("(Now Playing)")
        }

        pauseButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).pause()
        }

        stopButton.setOnClickListener {
            (requireActivity() as ControlFragment.ControlFragmentInterface).stop()
        }

    }

    // interface to hold abstract data to be implemented into main
    public interface ControlFragmentInterface {
        fun play()
        fun pause()
        fun stop()
    }
}