package edu.temple.audiobookplayer

import android.app.Service
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import edu.temple.audlibplayer.PlayerService

class ControlFragment : Fragment(){

    lateinit var bookViewModel: BookViewModel
    lateinit var playButton: Button
    val bindAudio = PlayerService()
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
        playButton = view.findViewById(R.id.button_play)
        playButton.setOnClickListener{
            (requireActivity() as ControlFragment.ControlFragmentInterface).play()
        }
    }

    public interface ControlFragmentInterface {
        fun play()
        fun pause()
        fun stop()
    }
}