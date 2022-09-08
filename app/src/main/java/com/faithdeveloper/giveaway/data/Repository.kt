package com.faithdeveloper.giveaway.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.*
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.NAME_PHONE_TYPE
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.NAME_PICTURE_TYPE
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.NAME_TYPE
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.PHONE_PICTURE_TYPE
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.PHONE_TYPE
import com.faithdeveloper.giveaway.ui.fragments.ProfileEdit.Companion.PICTURE_TYPE
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.checkTypeOfMedia
import com.faithdeveloper.giveaway.utils.Extensions.getTimelineOption
import com.faithdeveloper.giveaway.utils.Extensions.getUserDetails
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.setSignInStatus
import com.faithdeveloper.giveaway.utils.Extensions.storeTimelineOption
import com.faithdeveloper.giveaway.utils.Extensions.storeUserDetails
import com.faithdeveloper.giveaway.utils.Extensions.storeUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.NotificationUtil
import com.faithdeveloper.giveaway.viewmodels.FeedVM.Companion.DEFAULT_FILTER
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.*

class Repository(
    private val auth: FirebaseAuth,
    val context: Context
) {
    // init properties
    private var database: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var appState = APP_STARTED

    //    this variable stores the profile of a user that it about to be viewed in the Profile fragment
    private lateinit var authorProfileForProfileView: UserProfile

    //    the latest post uploaded by a user is stored in this variable
    private var uploadedPost: Post? = null

    fun checkUserRegistration() = auth.currentUser

    private fun database() = database ?: Firebase.firestore

    private fun storage() = storage ?: FirebaseStorage.getInstance()

    fun userUid() = auth.currentUser?.uid

    fun getTimelineOption() = context.getTimelineOption()

    fun userDetails() = context.getUserDetails()

    suspend fun signUp(phone: String, name: String, email: String, password: String): Event {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.i("GA", "Account creation successful")
            context.storeUserDetails(name, email, phone)
            Event.Success(data = null, msg = "Account creation successful")
        } catch (e: FirebaseAuthUserCollisionException) {
            Event.Failure(e)
        } catch (e: FirebaseNetworkException) {
            Event.Failure(e)
        }
    }

    suspend fun verifyEmail(): Event {
        return try {
            val actionSettingsCodeInfo = ActionCodeSettings.newBuilder()
            actionSettingsCodeInfo.apply {
                handleCodeInApp = false
                url = "https://faithdeveloper.page.link.verification"
                dynamicLinkDomain = "faithdeveloper.page.link"
                setAndroidPackageName("com.faithdeveloper.giveaway", true, "1")
            }
            auth.currentUser!!.sendEmailVerification(actionSettingsCodeInfo.build()).await()
            Log.i("GA", "Email is verified")
            Event.Success(null, "Email verified")
        } catch (e: Exception) {
            Log.e("GA", "Failed to verify email")
            Event.Failure(null, "Failed to verify email")
        }
    }

    fun emailIsVerified() = auth.currentUser?.isEmailVerified

    suspend fun forgotPassword(email: String): Event {
        return try {
            val actionCodeSettings = ActionCodeSettings.newBuilder()
            actionCodeSettings.apply {
                handleCodeInApp = false
                url = "https://faithdeveloper.page.link.pass"
                dynamicLinkDomain = "faithdeveloper.page.link"
                setAndroidPackageName("com.faithdeveloper.giveaway", true, "1")
            }
            auth.sendPasswordResetEmail(email, actionCodeSettings.build()).await()
            Log.i("GA", "Password email sent")
            Event.Success(null, "Password email sent")
        } catch (e: Exception) {
            Log.e("GA", "Password reset failed")
            Event.Failure(null, "Password reset failed")
        }
    }

    suspend fun signIn(email: String, password: String): Event {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Log.i("GA", "Sign in successful")
            val userDetails = userDetails()
            val reference = database().collection(USERS).document(userUid()!!).get().await()
            if (reference.exists()) {
                // user is already a verified user
                if (userDetails[USERNAME_INDEX] == null) {
//                    store user details if not found on device
                    val userProfile = reference.toObject<UserProfile>()
                    context.storeUserDetails(
                        userProfile!!.name,
                        userProfile.email,
                        userProfile.phoneNumber,
                    )
                    if (userProfile.profilePicUrl != "") context.storeUserProfilePicUrl(userProfile.profilePicUrl)
                }
            } else {
                if (emailIsVerified() == true) {
                    // signing in from normal process of sign up
                    if (userDetails[USERNAME_INDEX] != null) {
                        createUserProfile(
                            userDetails[USERNAME_INDEX]!!,
                            userDetails[PHONE_NUMBER_INDEX]!!,
                            userDetails[EMAIL_INDEX]!!
                        )
                    }
                } else {
                    throw Exception("Email unverified")
                }
            }
            context.setSignInStatus(true)
            Log.i("GA", "Created profile successfully")
            Event.Success(null, "Sign in successful")
        } catch (e: Exception) {
            Log.e("GA", e.message ?: "Sign in failed")
            Event.Failure(null, e.message ?: "Sign in failed")
        }
    }

    private suspend fun createUserProfile(
        name: String,
        phoneNumber: String,
        email: String,
    ) {
        database().collection(USERS).document(userUid()!!).set(
            UserProfile(
                userUid()!!,
                name,
                phoneNumber,
                email,
                "",
                reports = 0
            )
        ).await()
    }

    suspend fun createProfilePicture(profilePicPath: Uri): Event {
        return try {
            val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
            reference.putFile(profilePicPath).await()
            val downloadUrl = reference.downloadUrl.await()
            database().collection(USERS).document(userUid()!!)
                .update("profilePicUrl", downloadUrl.toString()).await()
            context.storeUserProfilePicUrl(downloadUrl.toString())
            Event.Success(null, "Profile pic update successful")
        } catch (e: Exception) {
            Log.e("GA", e.message!!)
            Event.Failure(null, "Profile pic update failed")
        }
    }

    suspend fun uploadNewPost(
        postText: String,
        mediaUriList: MutableList<Uri>,
        tags: HashMap<Int, Boolean>,
        hasComments: Boolean,
        link: String?
    ): Event {
        return try {
            var post: Post
            coroutineScope {
//                init all possible jobs
                var videoUploadJob: Deferred<String> = async {
                    return@async ""
                }
                val imageUploadJobList: MutableList<Deferred<String>> = mutableListOf()
//                collects the urls of uploaded media
                val mediaUrlsList: MutableList<String> = mutableListOf()
//                this variable is used when pushing the new post to the database
                var hasVideo = false

//                check type of media
                if (mediaUriList.isNotEmpty()) {
//                    media is part of post
//                      media is either video or image, so a check is done
                    val mediaType = context.checkTypeOfMedia(mediaUriList[0])

                    if (mediaType == "image") {
                        mediaUriList.forEach {
                            val job = async {
                                val reference = storage().getReference(POST_PHOTOS)
                                    .child("${userUid()}${System.currentTimeMillis()}")
                                reference.putFile(it).await()
                                val downloadUrl = reference.downloadUrl.await()
                                return@async downloadUrl.toString()
                            }
                            imageUploadJobList.add(job)
                        }
                    } else {
                        videoUploadJob = async {
                            return@async uploadPostVideo(mediaUriList[0]).toString()
                        }

                        hasVideo = true
                    }
                }
//                get media urls
                if (mediaUriList.isNotEmpty()) {
                    if (imageUploadJobList.isNotEmpty()) mediaUrlsList.addAll(imageUploadJobList.awaitAll())
                    else mediaUrlsList.add(videoUploadJob.await())
                }
                val timePosted = System.currentTimeMillis()
                post = Post(
                    authorId = userUid()!!,
                    postID = "${userUid()}$timePosted",
                    time = null,
                    text = postText,
                    tags = getTags(tags),
                    mediaUrls = mediaUrlsList,
                    hasComments = hasComments,
                    hasVideo = hasVideo,
                    link = link ?: "",
                    commentCount = 0
                )
            }
            //              push to the database
            database().collection(POSTS).document(post.postID).set(post)
            uploadedPost = post
            Event.Success(null)
        } catch (e: Exception) {
            Log.e("GA", e.message ?: "error")
            Event.Failure("Post Failed", e.message ?: "error")
        }
    }

    private fun getTags(tags: HashMap<Int, Boolean>): MutableList<String> {
        val result = mutableListOf<String>()
        val tagsData = context.resources.getStringArray(R.array.tags)
        tags.forEach {
            if (it.value) {
                result.add(tagsData[it.key])
            }
        }
        return result
    }

    private suspend fun uploadPostVideo(videoUri: Uri): Uri? {
        val reference = storage().getReference(POST_VIDEOS)
            .child("${userUid()}${System.currentTimeMillis()}")
        reference.putFile(videoUri).await()
        return reference.downloadUrl.await()
    }


    suspend fun getProfileFeed(
        key: PagerKey,
        authorId: String
    ): Event {
        //        set up query
//        'firebasePosts' is the returned posts in firebase model. It will be converted to the model @class Post
        val firebasePosts: QuerySnapshot = if (key.lastSnapshot == null) {
//            no previous data loaded
            database().collection(POSTS)
                .limit(key.loadSize)
                .whereEqualTo("authorId", authorId)
                .orderBy("time", Query.Direction.DESCENDING)
                .get().await()
        } else {
//            previous data has been loaded, continues from where it stopped last which is based on the last snapshot from the key
            database().collection(POSTS)
                .limit(key.loadSize)
                .orderBy("time", Query.Direction.DESCENDING)
                .whereEqualTo("authorId", authorId)
                .startAfter(key.lastSnapshot)
                .get().await()
        }
        val result: Event
        if (firebasePosts.size() > 0) {
//            found data
            coroutineScope {
                //     converts the posts from the firebase snapshot to @class Post
                val convertPostsJob =
                    async { convertSnapshotToClass<Post>(firebasePosts.documents) }

                //            result of above job
                val posts = convertPostsJob.await()

                result = Event.Success(PagerResponse(posts, firebasePosts.documents.last()))
            }
        } else {
            // no data or all data have been loaded from the database
            result = Event.Success(PagerResponse(emptyList<Post>(), null))
        }
        return result
    }

    suspend fun getFeed(key: PagerKey): Event {
//        set up query
//        'firebasePosts' is the returned posts in firebase model. It will be converted to the model @class Post
        val firebasePosts: QuerySnapshot = if (key.lastSnapshot == null) {
//            no previous data loaded
            if (key.filter == DEFAULT_FILTER) {
//        gets all posts
                database().collection(POSTS)
                    .limit(key.loadSize)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .get().await()
            } else {
//            get posts under specific tag
                database().collection(POSTS)
                    .limit(key.loadSize)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .whereArrayContains("tags", key.filter)
                    .get().await()
            }
        } else {
//            previous data has been loaded, continues from where it stopped last which is based on the last snapshot from the key
            if (key.filter == DEFAULT_FILTER) {
                database().collection(POSTS)
                    .limit(key.loadSize)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .startAfter(key.lastSnapshot)
                    .get().await()
            } else {
                database().collection(POSTS)
                    .limit(key.loadSize)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .startAfter(key.lastSnapshot)
                    .whereArrayContains("tags", key.filter)
                    .get().await()
            }
        }

        val result: Event
        if (firebasePosts.size() > 0) {
//            found data
            coroutineScope {
                //            get the data of those who made the posts and converts the posts from the firebase snapshot to @class Post
                val getAuthorDataAndConvertItJob =
                    async { getProfileOfPostAuthors(firebasePosts.documents) }
                val convertPostsJob =
                    async { convertSnapshotToClass<Post>(firebasePosts.documents) }

                //            results of above two jobs
                val authorData = getAuthorDataAndConvertItJob.await()
                val posts = convertPostsJob.await()

                //            combine above two results
                val feedDataList = combineAuthorDataAndPostsData(authorData, posts)
                result = Event.Success(PagerResponse(feedDataList, firebasePosts.documents.last()))
            }
        } else {
            // no data or all data have been loaded from the database
            result = Event.Success(PagerResponse(emptyList<FeedData>(), null))
        }
        return result
    }

    suspend fun getLatestFeed(timestamp: Long, filter: String): Event {
        var result: Event = Event.InProgress(null)
        return try {

            val firebasePosts = if (filter == DEFAULT_FILTER) {
                database().collection(POSTS)
                    .whereGreaterThan("time", Date(timestamp))
                    .limit(10)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .get().await()
            } else {
                database().collection(POSTS)
                    .whereGreaterThan("time", timestamp)
                    .whereArrayContains("tags", filter)
                    .limit(10)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .get().await()
            }
            if (firebasePosts.size() > 0) {
//            found data
                coroutineScope {
                    //            get the data of those who made the posts and converts the posts from the firebase snapshot to @class Post
                    val getAuthorDataAndConvertItJob =
                        async { getProfileOfPostAuthors(firebasePosts.documents) }
                    val convertPostsJob =
                        async { convertSnapshotToClass<Post>(firebasePosts.documents) }

                    //            results of above two jobs
                    val authorData = getAuthorDataAndConvertItJob.await()
                    val posts = convertPostsJob.await()

                    //            combine above two results
                    val feedDataList = combineAuthorDataAndPostsData(authorData, posts)
                    result =
                        Event.Success(PagerResponse(feedDataList, firebasePosts.documents.last()))
                }
            } else {
                result = Event.Success(null)// do nothing
            }
            result
        } catch (e: Exception) {
            result = Event.Failure(null)
            result
        }
    }

    private fun combineAuthorDataAndPostsData(
        authorData: List<UserProfile?>,
        posts: List<Post?>
    ): List<FeedData> {
        val result = authorData.mapIndexed { index, it ->
            FeedData(posts[index], it)

        }
        return result


    }

    private fun combineCommentsAndProfiles(
        authorData: List<UserProfile?>,
        comments: List<Comment?>
    ): List<CommentData> {
        val result = authorData.mapIndexed { index, it ->
            CommentData(
                comments[index],
                authorData[index]
            )
        }
        return result
    }

    private fun combineRepliesAndProfiles(
        profileData: List<ReplyProfiles?>,
        replies: List<Reply?>
    ): List<ReplyData> {
        val result = profileData.mapIndexed { index, it ->
            ReplyData(
                replies[index],
                profileData[index]?.author,
                profileData[index]?.userRepliedTo
            )
        }
        return result
    }

    private inline fun <reified T> convertSnapshotToClass(snapshots: List<DocumentSnapshot>): List<T?> {
        val result = snapshots.map {
            it.toObject<T>()
        }
        return result
    }

    private suspend fun getProfileOfPostAuthors(documents: List<DocumentSnapshot>): List<UserProfile?> {
        val jobList = mutableListOf<Deferred<UserProfile?>>()
        return coroutineScope {
            documents.forEach {
                val job = async {
//                    convert from firebase ,model to @class Post
                    val post = it.toObject<Post>()
                    return@async if (post?.authorId == userUid()) {
//                        author is this user, so fetch profile of  author from cached database
                        getUserProfile()
                    } else {
//                   author is not this user, so fetch profile of  author from remote database
                        val profile = database().collection(USERS).document(post?.authorId!!).get()
                            .await()
                        profile.toObject<UserProfile>()
                    }
                }
                jobList.add(job)
            }
            jobList.awaitAll()
        }
    }

    private suspend fun getProfileOfCommentAuthors(documents: List<DocumentSnapshot>): List<UserProfile?> {
//        this job list is for jobs for getting the profile of the author of a comment
        val authorOfCommentJobList = mutableListOf<Deferred<UserProfile?>>()
////        this job list is for jobs getting the profile of users comment is a reply to
//        val profileOfUserCommentIsAReplyToJobList = mutableListOf<Deferred<UserProfile?>>()

        return coroutineScope {
            documents.forEach {
//                convert from firebase ,model to @class Comment
                val comment = it.toObject<Comment>()
//                create job for authors
                val commentAuthorJob = async {
                    return@async if (comment?.authorId == userUid()!!) {
//                        author is this user, so fetch profile of  author from cached database
                        getUserProfile()
                    } else {
//                        author is not this user, so fetch profile of  author from remote database
                        val profile =
                            database().collection(USERS).document(comment?.authorId!!).get()
                                .await()
                        profile.toObject<UserProfile>()
                    }
                }
                authorOfCommentJobList.add(commentAuthorJob)
            }
            return@coroutineScope authorOfCommentJobList.awaitAll()
        }
    }

    private suspend fun getProfileOfAuthorsAndProfileOfUsersRepliedTo(documents: List<DocumentSnapshot>): List<ReplyProfiles?> {
//        this job list is for jobs for getting the profile of the author of a comment
        val authorOfCommentJobList = mutableListOf<Deferred<UserProfile?>>()
////        this job list is for jobs getting the profile of users comment is a reply to
        val profileOfUserCommentIsAReplyToJobList = mutableListOf<Deferred<UserProfile?>>()

        return coroutineScope {
            documents.forEach {
//                convert from firebase ,model to @class Comment
                val reply = it.toObject<Reply>()
//                create job for authors
                val replyAuthorJob = async {
                    return@async if (reply?.authorId == userUid()!!) {
//                        author is this user, so fetch profile of  author from cached database
                        getUserProfile()
                    } else {
//                        author is not this user, so fetch profile of  author from remote database
                        val profile =
                            database().collection(USERS).document(reply?.authorId!!).get()
                                .await()
                        profile.toObject<UserProfile>()
                    }
                }
                val profileOfUserRepliedToJob = async {
                    return@async when (reply?.idOfTheUserThisCommentIsAReplyTo) {
                        "" -> {
//                            this comment isn't replying anybody
                            null
                        }
                        userUid()!! -> {
//                            this comment is replying this user, so fetch profile of user from cached database
                            getUserProfile()
                        }
                        else -> {
//                            this comment is not replying this user, so fetch profile of user from remote database
                            val profile = database().collection(USERS)
                                .document(reply?.idOfTheUserThisCommentIsAReplyTo!!).get().await()
                            profile.toObject<UserProfile>()
                        }
                    }
                }
//                add respective jobs
                authorOfCommentJobList.add(replyAuthorJob)
                profileOfUserCommentIsAReplyToJobList.add(profileOfUserRepliedToJob)
            }

//            get results of respective jobs
            val listOfCommentAuthors = authorOfCommentJobList.awaitAll()
            val listOfUsersRepliedTo = profileOfUserCommentIsAReplyToJobList.awaitAll()

//            combine results of above jobs and return
            val result = listOfCommentAuthors.mapIndexed { index, userProfile ->
                ReplyProfiles(userProfile, listOfUsersRepliedTo[index])
            }
            return@coroutineScope result
        }
    }

    fun setTimeLineOption(timeLineOption: String) {
        context.storeTimelineOption(timeLineOption)
    }

    fun getUserProfilePicUrl(): String? = context.getUserProfilePicUrl()

    suspend fun getComments(key: PagerKey, postID: String): Event {
        //        set up query
//        'firebaseComments' is the returned comments in firebase model. It will be converted to the model @class Comment
        val firebaseComments: QuerySnapshot = if (key.lastSnapshot == null) {
//            no previous data loaded
            database().collection(POSTS)
                .document(postID)
                .collection(COMMENTS)
                .limit(key.loadSize)
                .orderBy("time", Query.Direction.DESCENDING)
                .get().await()

        } else {
//            previous data has been loaded, continues from where it stopped last which is based on the last snapshot from the key
            database().collection(POSTS)
                .document(postID)
                .collection(COMMENTS)
                .limit(key.loadSize)
                .orderBy("time", Query.Direction.DESCENDING)
                .startAfter(key.lastSnapshot)
                .get().await()
        }

        val result: Event
        if (firebaseComments.size() > 0) {
//            found data
            coroutineScope {
                //            get the profile of those who made the comments, profile of who the comment is a reply to  and converts the posts from the firebase snapshots to @class CommentProfile
                val getAuthorDataAndConvertItJob =
                    async { getProfileOfCommentAuthors(firebaseComments.documents) }
                val convertCommentsJob =
                    async { convertSnapshotToClass<Comment>(firebaseComments.documents) }

                //            results of above two jobs
                val authorProfiles = getAuthorDataAndConvertItJob.await()
                val comments = convertCommentsJob.await()

                //            combine above two results
                val commentDataList =
                    combineCommentsAndProfiles(authorProfiles, comments)
                result =
                    Event.Success(PagerResponse(commentDataList, firebaseComments.documents.last()))
            }
        } else {
            // no data or all data have been loaded from the database
            result = Event.Success(PagerResponse(emptyList<CommentData>(), null))
        }
        return result
    }

    suspend fun getReplies(key: PagerKey, parentId: String, commentId: String): Event {
        //        set up query
//        'firebaseComments' is the returned comments in firebase model. It will be converted to the model @class Comment
        val firebaseComments: QuerySnapshot = if (key.lastSnapshot == null) {
//            no previous data loaded
            database().collection(POSTS)
                .document(parentId)
                .collection(COMMENTS)
                .document(commentId)
                .collection(REPLIES)
                .limit(key.loadSize)
                .orderBy("time", Query.Direction.DESCENDING)
                .get().await()

        } else {
//            previous data has been loaded, continues from where it stopped last which is based on the last snapshot from the key
            database().collection(POSTS)
                .document(parentId)
                .collection(COMMENTS)
                .document(commentId)
                .collection(REPLIES)
                .limit(key.loadSize)
                .orderBy("time", Query.Direction.DESCENDING)
                .get().await()
        }

        val result: Event
        if (firebaseComments.size() > 0) {
//            found data
            coroutineScope {
                //            get the profile of those who made the comments, profile of who the comment is a reply to  and converts the posts from the firebase snapshots to @class CommentProfile
                val getAuthorDataAndConvertItJob =
                    async { getProfileOfAuthorsAndProfileOfUsersRepliedTo(firebaseComments.documents) }
                val convertRepliesJob =
                    async { convertSnapshotToClass<Reply>(firebaseComments.documents) }

                //            results of above two jobs
                val authorAndUserRepliedToProfiles = getAuthorDataAndConvertItJob.await()
                val replies = convertRepliesJob.await()

                //            combine above two results
                val replyDataList =
                    combineRepliesAndProfiles(authorAndUserRepliedToProfiles, replies)
                result =
                    Event.Success(PagerResponse(replyDataList, firebaseComments.documents.last()))
            }
        } else {
            // no data or all data have been loaded from the database
            result = Event.Success(PagerResponse(emptyList<ReplyData>(), null))
        }
        return result
    }

    fun signOut(): Event {
        return try {
            auth.signOut()
            Event.Success(null)
        } catch (e: Exception) {
            Log.e("GA", e.message ?: "Failed to sign out")
            Event.Failure(null)
        }
    }

    suspend fun updateProfile(
        profileEditType: String,
        phone: String?,
        name: String?,
        newPicture: Uri?
    ): Event {
        return try {
            when (profileEditType) {
                NAME_TYPE -> updateUserName(name)
                PHONE_TYPE -> updateUserPhone(phone)
                PICTURE_TYPE -> updatePicture(newPicture)
                NAME_PHONE_TYPE -> updateNameAndPhone(name, phone)
                NAME_PICTURE_TYPE -> updateNameAndPicture(name, newPicture)
                PHONE_PICTURE_TYPE -> updatePhoneAndPicture(phone, newPicture)
                else -> updateAll(name, phone, newPicture)
            }
            Event.Success(null)
        } catch (e: Exception) {
            Log.i("GA", "failed to updateProfile")
            Event.Failure(null)
        }
    }

    private suspend fun updateAll(name: String?, phone: String?, newPicture: Uri?) {
        coroutineScope {
            val allJobs = mutableListOf<Deferred<Any>>()
            val job1 = async {
                database().collection(USERS).document(userUid()!!).update("phoneNumber", phone)
                    .await()
            }
            allJobs.add(job1)
            val job2 = async {
                val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
                reference.putFile(newPicture!!).await()
                val downloadUrl = reference.downloadUrl.await()
                database().collection(USERS).document(userUid()!!)
                    .update("profilePicUrl", downloadUrl.toString()).await()
                context.storeUserProfilePicUrl(downloadUrl.toString())
            }
            allJobs.add(job2)
            val job3 = async {
                database().collection(USERS).document(userUid()!!).update("name", name!!).await()
            }
            allJobs.add(job3)
            allJobs.awaitAll()
        }
    }

    private suspend fun updatePhoneAndPicture(phone: String?, newPicture: Uri?) {
        coroutineScope {
            val allJobs = mutableListOf<Deferred<Any>>()
            val job1 = async {
                database().collection(USERS).document(userUid()!!).update("phoneNumber", phone)
                    .await()
            }
            allJobs.add(job1)
            val job2 = async {
                val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
                reference.putFile(newPicture!!).await()
                val downloadUrl = reference.downloadUrl.await()
                database().collection(USERS).document(userUid()!!)
                    .update("profilePicUrl", downloadUrl.toString()).await()
                context.storeUserProfilePicUrl(downloadUrl.toString())
            }
            allJobs.add(job2)
            allJobs.awaitAll()
        }
    }

    private suspend fun updateNameAndPicture(name: String?, newPicture: Uri?) {
        coroutineScope {
            val allJobs = mutableListOf<Deferred<Any>>()
            val job1 = async {
                database().collection(USERS).document(userUid()!!).update("name", name!!).await()
            }
            allJobs.add(job1)
            val job2 =
                async {
                    val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
                    reference.putFile(newPicture!!).await()
                    val downloadUrl = reference.downloadUrl.await()
                    database().collection(USERS).document(userUid()!!)
                        .update("profilePicUrl", downloadUrl.toString()).await()
                    context.storeUserProfilePicUrl(downloadUrl.toString())
                }
            allJobs.add(job2)
            allJobs.awaitAll()
        }
    }

    private suspend fun updateNameAndPhone(name: String?, phone: String?) {
        coroutineScope {
            val allJobs = mutableListOf<Deferred<Void>>()
            val job1 = async {
                database().collection(USERS).document(userUid()!!).update("name", name!!).await()
            }
            allJobs.add(job1)
            val job2 = async {
                database().collection(USERS).document(userUid()!!).update("phoneNumber", phone)
                    .await()
            }
            allJobs.add(job2)
            allJobs.awaitAll()
        }
    }

    private suspend fun updatePicture(newPicture: Uri?) {
        val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
        reference.putFile(newPicture!!).await()
        val downloadUrl = reference.downloadUrl.await()
        database().collection(USERS).document(userUid()!!)
            .update("profilePicUrl", downloadUrl.toString())
        context.storeUserProfilePicUrl(downloadUrl.toString())
    }

    private suspend fun updateUserPhone(phone: String?) {
        database().collection(USERS).document(userUid()!!).update("phoneNumber", phone)
    }

    private suspend fun updateUserName(name: String?) {
        database().collection(USERS).document(userUid()!!).update("name", name!!)
    }

    suspend fun formatFeedResponse(response: List<FirebaseModelFeed>): List<FeedData> {
        var result: List<FeedData>
        coroutineScope {
            val job = async {
                response.map {
                    FeedData(
                        it.postData?.toObject<Post>(),
                        it.authorData?.toObject<UserProfile>()
                    )
                }
            }
            result = job.await()
        }
        return result
    }

    suspend fun searchByTags(
        key: DocumentSnapshot?,
        searchKeyword: String,
        tag: String
    ): Event.Success {
        return Event.Success(null)
//        return if (key == null) {
//            val data = database().collection(POSTS)
//                .whereArrayContains("tags", searchKeyword)
//                .limit(30)
//                .get().await()
//            val authorData = getProfileOfPostAuthors(data.documents)
//            val resultList = data.documents.mapIndexed { index, snapShot ->
//                FirebaseModelFeed(snapShot, authorData[index])
//            }
//            Event.Success(resultList)
//        } else {
//            val data = database().collection(POSTS)
//                .whereArrayContains("tags", searchKeyword)
//                .limit(30)
//                .startAfter(key)
//                .get().await()
//            val authorData = getProfileOfPostAuthors(data.documents)
//            val resultList = data.documents.mapIndexed { index, snapShot ->
//                FirebaseModelFeed(snapShot, authorData[index])
//            }
//            Event.Success(resultList)
//        }
    }

    suspend fun deletePost(postID: String): Event {
        return try {
            database().collection(POSTS).document(postID).delete().await()
            Event.Success(null)
        } catch (e: Exception) {
            Event.Failure(null)
        }
    }

    private fun notifyUserOfNewComments(comments: List<CommentData>) {
        NotificationUtil.sendNotifications(context, comments)
    }

    fun setAppState(appState: String) {
        this.appState = appState
    }

    fun getUserProfile(): UserProfile {
        val userProfile = context.getUserDetails()
        return UserProfile(
            auth.uid!!,
            userProfile[0]!!,
            userProfile[1]!!,
            userProfile[2]!!,
            userProfile[3] ?: "",
            0
        )
    }

    fun getUploadedPost() = FeedData(
        uploadedPost!!,
        getUserProfile()
    )

    fun checkIfNewUploadedPostIsAvailable() = uploadedPost != null
    fun makeNewUploadedPostNull() {
        uploadedPost = null
    }


    fun setAuthorProfileForProfileView(authorProfile: UserProfile) {
        authorProfileForProfileView = authorProfile
    }

    fun getAuthorProfileForProfileView() = authorProfileForProfileView

    fun updateReply(
        newReply: String,
        parentId: String,
        commentId: String,
        replyId: String
    ): Event {
        return try {
            val map = mutableMapOf<String, Any?>(
                "commentText" to newReply,
                "updated" to true,
                "time" to null
            )
            database().collection(POSTS).document(parentId).collection(
                COMMENTS
            ).document(commentId).collection(REPLIES).document(replyId).update(map)
            Event.Success(newReply, "comment_edited")
        } catch (e: Exception) {
            Event.Failure(null, "comment_unedited")
        }
    }

     fun updateComment(newComment: String, parentId: String, commentId: String): Event {
        return try {
            val map = mutableMapOf<String, Any?>(
                "commentText" to newComment,
                "updated" to true,
                "time" to null
            )
            database().collection(POSTS).document(parentId).collection(
                COMMENTS
            ).document(commentId).update(map)
            Event.Success(newComment, "comment_edited")
        } catch (e: Exception) {
            Event.Failure(null, "comment_unedited")
        }
    }
    fun deleteComment(parentID: String, commentID: String, repliesCount:Int): Event {
        return try {
            val commentRef = database().collection(POSTS).document(parentID).collection(COMMENTS)
                .document(commentID)
            val parentRef = database().collection(POSTS).document(parentID)
            database().runBatch { batch ->
                val count = repliesCount.plus(1)
                batch.update(parentRef, "commentCount",FieldValue.increment(-count.toLong()))
                batch.delete(commentRef)
            }
            Event.Success("DELETED", "comment_deleted")
        } catch (e: Exception) {
            Event.Failure(null, "comment_undeleted")
        }
    }

     fun deleteReply(parentID: String, commentID: String, replyId: String): Event {
        return try {
            val commentRef = database().collection(POSTS).document(parentID).collection(COMMENTS)
                .document(commentID)
            val parentRef = database().collection(POSTS).document(parentID)
            val replyRef = database().collection(POSTS).document(parentID).collection(COMMENTS).document(commentID)
                .collection(
                    REPLIES
                ).document(replyId)
            database().runBatch { batch ->
                batch.update(parentRef, "commentCount", FieldValue.increment(-1))
                batch.update(commentRef, "replies", FieldValue.increment(-1))
                batch.delete(replyRef)
            }
            Event.Success("DELETED", "comment_deleted")
        } catch (e: Exception) {
            Event.Failure(null, "comment_undeleted")
        }
    }

     fun addAReply(
        replyText: String,
        parentId: String,
        commentId: String,
        profileOfUserThisCommentIsAReplyTo: UserProfile?
    ): Event {
        return try {
            val timePosted = System.currentTimeMillis()
            val reply = Reply(
                userUid()!!,
                replyText,
                "${userUid()!!}${timePosted}",
                null,
                profileOfUserThisCommentIsAReplyTo?.id ?: "",
                false
            )
            val commentRef = database().collection(POSTS).document(parentId).collection(COMMENTS)
                .document(commentId)
            val parentRef = database().collection(POSTS).document(parentId)
            val replyRef = database().collection(POSTS).document(parentId).collection(COMMENTS).document(commentId)
                .collection(
                    REPLIES
                ).document(reply.id)
            database().runBatch { batch ->
                batch.update(commentRef, "replies", FieldValue.increment(1))
                batch.update(parentRef, "commentCount", FieldValue.increment(1))
                batch.set(replyRef, reply)
            }
            Event.Success(
                ReplyData(
                    reply,
                    getUserProfile(),
                    profileOfUserThisCommentIsAReplyTo
                ), "comment_added"
            )
        } catch (e: Exception) {
            Event.Failure(null, "comment_unadded")
        }


    }

     fun addNewComment(
        commentText: String,
        parentId: String,
    ): Event {
        return try {
            val timePosted = System.currentTimeMillis()
            val comment = Comment(
                userUid()!!,
                commentText,
                "${userUid()!!}${timePosted}",
                null,
                parentId,
                0,
                false
            )
            val commentRef = database().collection(POSTS).document(parentId).collection(COMMENTS)
                .document(comment.id)
            val parentRef = database().collection(POSTS).document(parentId)
            database().runBatch { batch ->
                batch.set(commentRef, comment)
                batch.update(parentRef, "commentCount", FieldValue.increment(1))
            }
            Event.Success(
                CommentData(
                    comment,
                    getUserProfile()
                ), "comment_added"
            )
        } catch (e: Exception) {
            Event.Failure(null, "comment_unadded")
        }
    }

    suspend fun compressImage() {
        coroutineScope {

        }

    }


    companion object {
        const val POST_DATA = "post_data"
        const val APP_URL = "https://faithdeveloper.page.link.pass"
        const val POSTS = "posts"
        const val USERS = "users"
        const val USERNAME_INDEX = 0
        const val EMAIL_INDEX = 2
        const val PHONE_NUMBER_INDEX = 1
        const val PROFILE_PHOTOS = "Profile_pictures"
        const val POST_PHOTOS = "Post_photos"
        const val POST_VIDEOS = "Post_videos"
        const val APP_STARTED = "app_started"
        const val APP_PAUSED = "app_paused"
        const val COMMENTS = "comments"
        const val REPLIES = "replies"
    }
}