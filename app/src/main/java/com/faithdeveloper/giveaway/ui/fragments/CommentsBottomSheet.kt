package com.faithdeveloper.giveaway.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutCommentsBinding
import com.faithdeveloper.giveaway.ui.adapters.CommentsPagerAdapter
import com.faithdeveloper.giveaway.ui.adapters.NewCommentsAdapter
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.faithdeveloper.giveaway.viewmodels.CommentsVM
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel: CommentsVM
    private lateinit var adapter: CommentsPagerAdapter
    private lateinit var newCommentsAdapter: NewCommentsAdapter
    private lateinit var arrayOfNewComments: MutableList<CommentData>
    private lateinit var concatAdapter: ConcatAdapter
    private var _binding: LayoutCommentsBinding? = null
    private val binding get() = _binding!!
    private var _dialogBuilder: MaterialAlertDialogBuilder? = null
    private var _dialog: AlertDialog? = null

    //    this is interface is used to communicate with the main fragment
    private var fragmentCommentsInterface: FragmentCommentsInterface? = null
    private var newFragmentCommentsInterface: FragmentCommentsInterface? = null
    private var profileOfAuthorBeingReplied: UserProfile? = null
    private var idOfPostThatIsCommented: String = ""
    private var action: String = POST

    override fun onCreate(savedInstanceState: Bundle?) {

//        init view model
        viewModel = ViewModelProvider(
            this@CommentsBottomSheet,
            VMFactory(
                (activity as MainActivity).getRepository(),
                arguments?.getString(POST_ID)
            )
        ).get(CommentsVM::class.java)

        setUpAdapter()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //        setup dialog view
        (dialog as? BottomSheetDialog)?.behavior?.isFitToContents = false
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        (dialog as? BottomSheetDialog)?.behavior?.isHideable = false

        binding.commentRecycler.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.commentRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.commentRecycler.adapter = concatAdapter

        handleObserver()
        setUpLoadState()
        updateSendButtonStatus()
//        watchCommentBox()
//        sendNewComment()
        closeDialog()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateSendButtonStatus() {
//        this is used before the textbox watcher is activated
//        if (binding.commentsLayout.editText?.text.toString().isBlank()) binding.send.disable()
//        else binding.send.enable()
    }

    private fun handleObserver() {
//        observes the result of user actions such as add, edit or delete a comment
        viewModel.commentActionResult.observe(viewLifecycleOwner) {
            when (it) {
                is Event.Success -> {
                    _dialog?.dismiss()
                    when (it.msg) {
                        "comment_added" -> {
                            action = ""
                            _dialog?.dismiss()
                            arrayOfNewComments.add(it.data as CommentData)
                            newCommentsAdapter.notifyItemInserted(arrayOfNewComments.size + 1)
                        }
                        "comment_deleted" -> {
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Your comment has been removed"
                            )
                            adapter.removeComment()
                            dismiss()
                        }
                        "comment_edited" -> {
                            action = ""
                            _dialog?.dismiss()
                            adapter.updateComment(it.data as String)
                        }
                    }
                    cleanUpAfterUserAction()
                }
                is Event.Failure -> {
                    failedOperation(it.msg)
                }
                else -> {
                    // do nothing
                }
            }
        }

//        observe the comment feed
        viewModel.feedResult.observe(viewLifecycleOwner) {
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }
    }

    private fun setUpAdapter() {
        adapter = CommentsPagerAdapter(
            profileNameClick = { author: UserProfile ->
                fragmentCommentsInterface?.onClick(author)
            },
            reply = { profileOfTheAuthorBeingReplied: UserProfile ->
                setUpReply(profileOfTheAuthorBeingReplied)
            },
            viewModel.userUid(),
            moreClick = { action: String, postID: String, postText: String ->
                this.idOfPostThatIsCommented = postID
                when (action) {
                    UPDATE -> {
                        setUpEditOfComment(postID, postText)
                    }
                    DELETE -> {
                        deleteComment(postID)
                    }
                }
            }
        )

//        set up new comments adapter
        arrayOfNewComments = mutableListOf()
        newCommentsAdapter =
            NewCommentsAdapter(arrayOfNewComments,
                profileNameClick = { poster: UserProfile ->
                    newFragmentCommentsInterface?.onClick(poster)
                },
                reply = { _profileOfAuthorBeingReplied: UserProfile ->
                    setUpReply(_profileOfAuthorBeingReplied)
                },
                viewModel.userUid(),
                moreClick = { action: String, postID: String, postText: String ->
                    this.idOfPostThatIsCommented = postID
                    when (action) {
                        UPDATE -> {
                            setUpEditOfComment(postID, postText)
                        }
                        DELETE -> {
                            deleteComment(postID)
                        }
                    }
                })
        concatAdapter = ConcatAdapter(newCommentsAdapter, adapter)
    }

    private fun setUpLoadState() {
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.source.refresh is LoadState.NotLoading && loadStates.append.endOfPaginationReached && adapter.itemCount == 0) {
                    makeEmptyResultLayoutVisible()
                } else {
                    makeEmptyResultLayoutInvisible()
                    when (loadStates.refresh) {
                        is LoadState.Error -> {
                            // initial load failed
                            makeErrorLayoutVisible()
                            binding.errorLayout.progressCircular.makeInVisible()
                        }
                        is LoadState.Loading -> {
                            // initial load has begun
                            binding.errorLayout.progressCircular.makeVisible()
                            makeErrorLayoutInvisible()
                        }
                        is LoadState.NotLoading -> {
                            binding.errorLayout.progressCircular.makeInVisible()
                            makeErrorLayoutInvisible()
                            Log.i(getString(R.string.app_name), "Not loading feed")
                        }
                    }

                    when (loadStates.append) {
                        is LoadState.Error -> {
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Failed to retrieve more feed"
                            )
                        }
                        is LoadState.Loading -> {
                            // additional load has begun
                        }
                        is LoadState.NotLoading -> {
                            if (loadStates.append.endOfPaginationReached) {
                                // all data has been loaded
                            }
                            if (loadStates.refresh is LoadState.NotLoading) {
                                // the previous load either initial or additional completed
                            }
                        }
                    }
                }
            }
        }
    }

//    private fun sendNewComment() {
//        binding.send.setOnClickListener {
////            dismiss any showing dialog
//            if (action != UPDATE) action = POST
//            userFeedback()
//            hideKeyboard()
//            viewModel.addOrUpdateComment(
//                binding.commentsLayout.editText?.text.toString().trim(),
//                profileOfAuthorBeingReplied,
//                action
//            )
//        }
//    }

    private fun userFeedback() {
        _dialog?.dismiss()
        //            create and show new dialog
        _dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = when (action) {
                POST -> "Adding your comment..."
                UPDATE -> "Updating your comment"
                else -> "Removing your comment..."
            }
        )
        _dialog = _dialogBuilder?.create()
        _dialog?.show()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken,
            InputMethodManager.RESULT_UNCHANGED_SHOWN
        )
    }

    private fun showKeyboard() {
//        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
//        binding.commentsLayout.editText?.requestFocus()
//        val inputManager =
//            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        inputManager.showSoftInput(
//            binding.commentsLayout.editText,
//            InputMethodManager.SHOW_IMPLICIT
//        )
    }

    private fun setUpReply(_profileOfAuthorBeingReplied: UserProfile) {
        profileOfAuthorBeingReplied = _profileOfAuthorBeingReplied
        showKeyboard()
    }

    private fun deleteComment(postID: String) {
        action = DELETE
        userFeedback()
        viewModel.deleteComment(postID)
    }

    private fun setUpEditOfComment(postID: String, postText: String) {
        action = UPDATE
        this.idOfPostThatIsCommented = postID
//        binding.commentsText.setText(postText)
        showKeyboard()
    }

    private fun failedOperation(operationType: String) {
        _dialog?.dismiss()
        requireContext().showSnackbarShort(
            binding.root, when (operationType) {
                "comment_unadded" -> "Failed to add comment"
                "comment-unedited" -> "Failed to update your comment"
                else -> "Failed to delete your comment"
            }
        )
        cleanUpAfterUserAction()
    }

    private fun makeErrorLayoutInvisible() {
        binding.errorLayout.errorText.makeInVisible()
        binding.errorLayout.retryButton.makeInVisible()
        binding.errorLayout.errorImage.makeInVisible()
    }

    private fun makeErrorLayoutVisible() {
        makeEmptyResultLayoutInvisible()
        binding.errorLayout.errorText.makeVisible()
        binding.errorLayout.retryButton.makeVisible()
        binding.errorLayout.errorImage.makeVisible()
    }

    private fun makeEmptyResultLayoutVisible() {
        makeErrorLayoutInvisible()
        binding.errorLayout.progressCircular.makeInVisible()
        binding.emptyResultLayout.emptyText.makeVisible()
    }

    private fun makeEmptyResultLayoutInvisible() {
        binding.emptyResultLayout.emptyText.makeInVisible()
    }

    private fun closeDialog() {
        binding.dismiss.setOnClickListener {
            dialog?.dismiss()
        }
    }

    companion object {
        const val UPDATE = "update"
        const val DELETE = "delete"
        const val POST = "post"
        const val TAG = "CommentsBottomSheet"
        private const val POST_ID = "commentsPath"
        private const val POSTER_ID = "poster_id"
        fun instance(
            postID: String,
            posterID: String,
            fragmentCommentsInterface: FragmentCommentsInterface?
        ): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                this.fragmentCommentsInterface = fragmentCommentsInterface
                this.newFragmentCommentsInterface = fragmentCommentsInterface
                arguments = Bundle().apply {
                    putString(POST_ID, postID)
                    putString(POSTER_ID, posterID)
                }
            }
        }
    }

    private fun cleanUpAfterUserAction() {
//        binding.commentsLayout.editText?.setText("")
//        binding.commentsLayout.editText?.clearFocus()
        action = ""
    }

    override fun onDestroyView() {
        binding.commentRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}