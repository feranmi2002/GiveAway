package com.faithdeveloper.giveaway.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutCommentsBinding
import com.faithdeveloper.giveaway.databinding.WriteCommentLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.CommentsPagerAdapter
import com.faithdeveloper.giveaway.ui.adapters.NewCommentsAdapter
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.faithdeveloper.giveaway.viewmodels.CommentsVM
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
    private var parentID: String = ""
    private var commentsCount: Int = 0

    private lateinit var writeCommentDialog: BottomSheetDialog
    private var WHICH_ADAPTER_IS_TAKING_ACTION = -1

    override fun onCreate(savedInstanceState: Bundle?) {

//        init view model
        viewModel = ViewModelProvider(
            this@CommentsBottomSheet,
            VMFactory(
                (activity as MainActivity).getRepository(),
                arguments?.getString(PARENT_ID)
            )
        ).get(CommentsVM::class.java)
        arguments?.let {
            parentID = it.getString(PARENT_ID)!!
            commentsCount = it.getInt(COMMENTS_COUNT)
        }

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //        setup dialog view
        (dialog as? BottomSheetDialog)?.behavior?.isHideable = true
        (dialog as? BottomSheetDialog)?.behavior?.isDraggable = true
        (dialog as? BottomSheetDialog)?.setCanceledOnTouchOutside(false)

        binding.commentRecycler.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.commentRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.commentRecycler.adapter = concatAdapter

        binding.commentRecycler.setOnTouchListener { mview, motionEvent ->
            mview.parent.parent.requestDisallowInterceptTouchEvent(true)
            mview.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }

        binding.count.text = commentsCount.toString()

        handleObserver()
        setUpLoadState()
        closeDialog()
        addNewComment()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        Glide.with(requireContext())
            .load(requireContext().getUserProfilePicUrl())
            .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
            .into(binding.profile)
        super.onStart()
    }

    private fun handleObserver() {
//        observes the result of user actions such as add, edit or delete a comment
        viewModel.commentActionResult.observe(viewLifecycleOwner) {
            when (it) {
                is Event.Success -> {
                    _dialog?.dismiss()
                    when (it.msg) {
                        "comment_added" -> {
                            _dialog?.dismiss()
                            writeCommentDialog.dismiss()
                            arrayOfNewComments.add(0, it.data as CommentData)
                            newCommentsAdapter.notifyItemInserted(arrayOfNewComments.size + 1)
                            makeEmptyResultLayoutInvisible()
                            makeErrorLayoutInvisible()
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Comment Added"
                            )
                        }
                        "comment_deleted" -> {
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Your comment has been removed"
                            )
                            if (WHICH_ADAPTER_IS_TAKING_ACTION == NEW_ADAPTER) newCommentsAdapter.removeComment()
                            else adapter.removeComment()
                        }
                        "comment_edited" -> {
                            _dialog?.dismiss()
                            writeCommentDialog.dismiss()
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Comment updated"
                            )
                        }
                    }

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
                fragmentCommentsInterface?.onClickUser(author)
                dismissAllowingStateLoss()
            },
            reply = { commentId: String, text, count: Int ->
                openReplyDialog(commentId, text, count)
            },
            viewModel.userUid(),
            deleteComment = { commentID: String, replies: Int ->
                WHICH_ADAPTER_IS_TAKING_ACTION = PAGER_ADAPTER
                deleteComment(commentID, replies)
            },
            editComment = { commentID: String, commentText: String ->
                WHICH_ADAPTER_IS_TAKING_ACTION = PAGER_ADAPTER
                setUpEditOfComment(commentID, commentText)
            }
        )

//        set up new comments adapter
        arrayOfNewComments = mutableListOf()
        newCommentsAdapter =
            NewCommentsAdapter(arrayOfNewComments,
                profileNameClick = { author: UserProfile ->
                    fragmentCommentsInterface?.onClickUser(author)
                    dismissAllowingStateLoss()
                },
                reply = { commentId: String, text, count: Int ->
                    openReplyDialog(commentId, text, count)
                },
                viewModel.userUid(),
                moreClick = { action: String, commentID: String, commentText: String, replies: Int ->
                    when (action) {
                        UPDATE -> {
                            WHICH_ADAPTER_IS_TAKING_ACTION = NEW_ADAPTER
                            setUpEditOfComment(commentID, commentText)
                        }
                        DELETE -> {
                            WHICH_ADAPTER_IS_TAKING_ACTION = NEW_ADAPTER
                            deleteComment(commentID, replies)
                        }
                    }
                })
        concatAdapter = ConcatAdapter(newCommentsAdapter, adapter)
    }

    private fun openReplyDialog(commentId: String, text: String, count: Int) {
        val replyDialog =
            RepliesBottomSheet.instance(parentID, commentId, fragmentCommentsInterface, text, count)
        replyDialog.show(requireActivity().supportFragmentManager, RepliesBottomSheet.TAG)
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

    private fun addNewComment() {
        binding.commentIdentifier.setOnClickListener {
            val binding = writeCommentDialog()
            writeCommentDialog.show()
            showKeyboard(binding.root)
            binding.send.setOnClickListener {
                hideKeyboard(binding.root)
                POST.userFeedback()
                viewModel.addNewComment(binding.textInputLayout.editText?.text.toString().trim())
            }
        }
    }

    private fun writeCommentDialog(): WriteCommentLayoutBinding {
        writeCommentDialog = BottomSheetDialog(requireContext())
        val binding = WriteCommentLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        binding.dismiss.setOnClickListener {
            hideKeyboard(binding.root)
            writeCommentDialog.dismiss()
        }
        binding.send.isEnabled = !binding.textInputLayout.editText?.text?.isBlank()!! == true
        binding.textInputLayout.editText?.doAfterTextChanged {
            binding.send.isEnabled = !it?.isBlank()!! == true
        }
        writeCommentDialog.setContentView(binding.root)
        writeCommentDialog.dismissWithAnimation = true
        writeCommentDialog.setCancelable(false)
        writeCommentDialog.setCanceledOnTouchOutside(false)
        return binding
    }

    private fun String.userFeedback() {
        _dialog?.dismiss()
        //            create and show new dialog
        _dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = when (this) {
                POST -> "Adding your comment..."
                UPDATE -> "Updating your comment"
                else -> "Removing your comment..."
            }
        )
        _dialog = _dialogBuilder?.create()
        _dialog?.show()
    }

    private fun hideKeyboard(binding: View) {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.windowToken,
            InputMethodManager.RESULT_UNCHANGED_SHOWN
        )
    }

    private fun showKeyboard(binding: View) {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding, InputMethodManager.RESULT_UNCHANGED_SHOWN)
    }

    private fun deleteComment(commentID: String, replies: Int) {
        DELETE.userFeedback()
        viewModel.deleteComment(commentID, replies)
    }

    private fun setUpEditOfComment(commentId: String, comment: String): String {
        var newComment = comment
        val binding = writeCommentDialog()
        binding.textInputLayout.editText?.setText(comment)
        writeCommentDialog.show()
        binding.send.setOnClickListener {
            hideKeyboard(binding.root)
            UPDATE.userFeedback()
            newComment = binding.textInputLayout.editText?.text.toString().trim()
                viewModel.editComment(
                commentId,
                newComment
            )
        }
        return newComment
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
        binding.emptyResultLayout.emptyImg.makeVisible()
    }

    private fun makeEmptyResultLayoutInvisible() {
        binding.emptyResultLayout.emptyText.makeInVisible()
        binding.emptyResultLayout.emptyImg.makeInVisible()
    }

    private fun closeDialog() {
        binding.dismiss.setOnClickListener {
            dialog?.dismiss()
        }
    }

    companion object {
        const val NEW_ADAPTER = 0
        const val PAGER_ADAPTER = 1
        const val UPDATE = "update"
        const val DELETE = "delete"
        const val POST = "post"
        const val TAG = "CommentsBottomSheet"
        const val PARENT_ID = "commentsPath"
        const val COMMENTS_COUNT = "commentCount"
        fun instance(
            parentId: String,
            commentsCount: Int,
            fragmentCommentsInterface: FragmentCommentsInterface?
        ): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                this.fragmentCommentsInterface = fragmentCommentsInterface
                this.newFragmentCommentsInterface = fragmentCommentsInterface
                arguments = Bundle().apply {
                    putString(PARENT_ID, parentId)
                    putInt(COMMENTS_COUNT, commentsCount)
                }
            }
        }
    }


    override fun onDestroyView() {
        binding.commentRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
