package com.faithdeveloper.giveaway.ui.fragments

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
import com.faithdeveloper.giveaway.data.models.ReplyData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutRepliesBinding
import com.faithdeveloper.giveaway.databinding.WriteCommentLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.NewReplyAdapter
import com.faithdeveloper.giveaway.ui.adapters.ReplyPagerAdapter
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.DELETE
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.PARENT_ID
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.POST
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.UPDATE
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.faithdeveloper.giveaway.viewmodels.RepliesVM
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class RepliesBottomSheet() : BottomSheetDialogFragment() {
    private lateinit var viewModel: RepliesVM
    private lateinit var adapter: ReplyPagerAdapter
    private lateinit var newReplyAdapter: NewReplyAdapter
    private lateinit var arrayOfNewReplies: MutableList<ReplyData>
    private lateinit var concatAdapter: ConcatAdapter
    private var _binding: LayoutRepliesBinding? = null
    private val binding get() = _binding!!
    private var _dialogBuilder: MaterialAlertDialogBuilder? = null
    private var _dialog: AlertDialog? = null

    //    this is interface is used to communicate with the main fragment
    private var fragmentCommentsInterface: FragmentCommentsInterface? = null
    private var newFragmentCommentsInterface: FragmentCommentsInterface? = null
    private lateinit var parentId: String
    private lateinit var commentId: String
    private lateinit var comment: String
    private var count by Delegates.notNull<Int>()

    private lateinit var writeCommentDialog: BottomSheetDialog
    private var WHICH_ADAPTER_IS_TAKING_ACTION = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            parentId = it.getString(PARENT_ID)!!
            commentId = it.getString(COMMENT_ID)!!
            comment = it.getString(COMMENT)!!
            count = it.getInt(REPLIES_COUNT, 0)
        }
//        init view model
        viewModel = ViewModelProvider(
            this@RepliesBottomSheet,
            VMFactory(
                (activity as MainActivity).getRepository()
            )
        ).get(RepliesVM::class.java)

        viewModel.setNeededData(parentId, commentId)
        arrayOfNewReplies = mutableListOf()
        setUpAdapter()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutRepliesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //        setup dialog view
        (dialog as? BottomSheetDialog)?.behavior?.isHideable = true
        (dialog as? BottomSheetDialog)?.behavior?.isDraggable = true
        (dialog as? BottomSheetDialog)?.setCanceledOnTouchOutside(false)

        binding.comment.text = comment
        binding.count.text = count.toString()

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
        closeDialog()
        addNewReply()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        Glide.with(requireContext())
            .load(requireContext().getUserProfilePicUrl())
            .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
            .into(binding.profile)
        super.onStart()
    }

    private fun addNewReply() {
        binding.replyIdentifier.setOnClickListener {
            setUpNewReply()
        }
    }

    private fun setUpAdapter() {
        adapter = ReplyPagerAdapter(
            profileNameClick = { author: UserProfile ->
                fragmentCommentsInterface?.onClickUser(author)
            },
            reply = {
                setUpReply(it)
            },
            viewModel.userUid(),
            moreClick = { action: String, id: String, replyText: String ->
                WHICH_ADAPTER_IS_TAKING_ACTION = PAGER_ADAPTER
                when (action) {
                    CommentsBottomSheet.UPDATE -> {
                        setUpEditOfComment(id, replyText)
                    }
                    CommentsBottomSheet.DELETE -> {
                        deleteComment(id)
                    }
                }
            })
        newReplyAdapter = NewReplyAdapter(
            arrayOfNewReplies,
            profileNameClick = { author: UserProfile ->
                fragmentCommentsInterface?.onClickUser(author)
            },
            reply = {
                setUpReply(it)
            },
            viewModel.userUid(),
            moreClick = { action: String, id: String, postText: String ->
                WHICH_ADAPTER_IS_TAKING_ACTION = NEW_ADAPTER
                when (action) {
                    UPDATE -> {
                        setUpEditOfComment(id, postText)
                    }
                    DELETE -> {
                        deleteComment(id)
                    }
                }
            })
        concatAdapter = ConcatAdapter(newReplyAdapter, adapter)
    }

    private fun deleteComment(id: String) {
        _dialogBuilder = requireContext().showDialog(false,
            title = "Delete Reply?",
            positiveButtonText = "DELETE",
            positiveAction = {
                DELETE.userFeedback()
                viewModel.deleteReply(id)
            },
            negativeButtonText = "CANCEL",
            negativeAction = {
                _dialog?.dismiss()
            })
        _dialog = _dialogBuilder?.create()
        _dialog?.show()
    }

    private fun setUpNewReply() {
        val binding = spinUpTextLayoutDialog()
        writeCommentDialog.show()
        binding.send.setOnClickListener {
            hideKeyboard()
            POST.userFeedback()
            viewModel.addNewReply(binding.textInputLayout.editText?.text.toString().trim())
        }
    }


    private fun setUpEditOfComment(id: String, comment: String) {
        val binding = spinUpTextLayoutDialog()
        binding.textInputLayout.editText?.setText(comment)
        writeCommentDialog.show()
        binding.send.setOnClickListener {
            hideKeyboard()
            UPDATE.userFeedback()
            viewModel.editReply(
                id,
                binding.textInputLayout.editText?.text.toString().trim()
            )
        }
    }

    private fun setUpReply(profileOfUserReplied: UserProfile?) {
        val binding = spinUpTextLayoutDialog()
        writeCommentDialog.show()
        binding.send.setOnClickListener {
            hideKeyboard()
            POST.userFeedback()
            viewModel.uploadReply(
                profileOfUserReplied,
                binding.textInputLayout.editText?.text.toString().trim()
            )
        }
    }

    private fun spinUpTextLayoutDialog(): WriteCommentLayoutBinding {
        writeCommentDialog = BottomSheetDialog(requireContext())
        val binding = WriteCommentLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        binding.dismiss.setOnClickListener {
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

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken,
            InputMethodManager.RESULT_UNCHANGED_SHOWN
        )
    }


    private fun handleObserver() {
//        observes the result of user actions such as add, edit or delete a comment
        viewModel.actionResult.observe(viewLifecycleOwner) {
            when (it) {
                is Event.Success -> {
                    _dialog?.dismiss()
                    when (it.msg) {
                        "comment_added" -> {
                            _dialog?.dismiss()
                            writeCommentDialog.dismiss()
                            arrayOfNewReplies.add(0, it.data as ReplyData)
                            newReplyAdapter.notifyItemInserted(arrayOfNewReplies.size + 1)
                            makeEmptyResultLayoutInvisible()
                            makeErrorLayoutInvisible()
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Reply Added"
                            )
                        }
                        "comment_deleted" -> {
                            _dialog?.dismiss()
                            if (WHICH_ADAPTER_IS_TAKING_ACTION == NEW_ADAPTER) newReplyAdapter.removeReply()
                            else adapter.removeReply()
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Your reply has been removed"
                            )
                        }
                        "comment_edited" -> {
                            _dialog?.dismiss()
                            writeCommentDialog.dismiss()
                            if (WHICH_ADAPTER_IS_TAKING_ACTION == NEW_ADAPTER) newReplyAdapter.updateReply(
                                it.data as String
                            )
                            else adapter.updateReply(it.data as String)
                            requireContext().showSnackbarShort(
                                binding.root,
                                "Reply Updated"
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

    private fun failedOperation(operationType: String) {
        _dialog?.dismiss()
        requireContext().showSnackbarShort(
            binding.root, when (operationType) {
                "comment_unadded" -> "Failed to add reply"
                "comment-unedited" -> "Failed to update your reply"
                else -> "Failed to delete your reply"
            }
        )
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

    private fun String.userFeedback() {
        _dialog?.dismiss()
        //            create and show new dialog
        _dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = when (this) {
                CommentsBottomSheet.POST -> "Adding your reply..."
                CommentsBottomSheet.UPDATE -> "Updating your reply"
                else -> "Removing your reply"
            }
        )
        _dialog = _dialogBuilder?.create()
        _dialog?.show()
    }


    companion object {
        const val NEW_ADAPTER = 0
        const val PAGER_ADAPTER = 1
        const val COMMENT_ID = "commentId"
        const val COMMENT = "comment"
        const val REPLIES_COUNT = "replies_count"
        const val TAG = "RepliesBottomSheet"
        fun instance(
            parentId: String,
            commentId: String,
            fragmentCommentsInterface: FragmentCommentsInterface?,
            comment: String,
            count: Int
        ): RepliesBottomSheet {
            return RepliesBottomSheet().apply {
                this.fragmentCommentsInterface = fragmentCommentsInterface
                this.newFragmentCommentsInterface = fragmentCommentsInterface
                arguments = Bundle().apply {
                    putString(CommentsBottomSheet.PARENT_ID, parentId)
                    putString(COMMENT_ID, commentId)
                    putString(COMMENT, comment)
                    putInt(REPLIES_COUNT, count)
                }
            }
        }
    }
}