package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.data.ChatRoom.cache.MessageCacheManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.SeparateUserCacheManager
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.dialog.CropDialog
import com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.HomePageFeatures.InformationAdapterView

// 用户个人主页
class HomePage(
    private val context: Context,
    private val userData: UserData,
    private val userInformation: UserInformation,
) {
    private lateinit var mainView: LinearLayout
    private lateinit var cropDialog: CropDialog
    private lateinit var informationAdapterView: InformationAdapterView
    private var homePage: LinearLayout? = null
    private var pageRecyclerView: RecyclerView? = null
    private val userCacheManager by lazy { SeparateUserCacheManager(context) }
    private val userMessageCacheManager by lazy { MessageCacheManager(context, userData.userId) }

    private val interaction by lazy {
        Interaction(context)
    }

    fun interface onSignOutListener {
        fun onSignOut()
    }

    private var signOutListener: onSignOutListener? = null

    fun setOnSignOutListener(listener: onSignOutListener) {
        this.signOutListener = listener
    }

    fun interface onExitToLoginListener {
        fun onExitToLogin()
    }

    private var exitToLoginListener: onExitToLoginListener? = null

    fun setOnExitToLoginListener(listener: onExitToLoginListener) {
        this.exitToLoginListener = listener
    }

    fun interface onUserImageChangeListener {
        fun onUserImageChange()
    }

    private var userImageChangeListener: onUserImageChangeListener? = null

    fun setOnUserImageChangeListener(listener: onUserImageChangeListener) {
        this.userImageChangeListener = listener
    }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

            createUserHomePageView()
        }
        return mainView
    }

    private fun LinearLayout.createUserHomePageView() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = VERTICAL

            informationAdapterView = InformationAdapterView(
                context,
                userData,
                userInformation,
                interaction,
                UserDataPref(context),
                userCacheManager,
                userMessageCacheManager,
                {
                    signOutListener?.onSignOut()
                },
                {
                    exitToLoginListener?.onExitToLogin()
                },
                {
                    userImageChangeListener?.onUserImageChange()
                },
                { bitmap, callback ->
                    cropDialog = CropDialog(context)
                    showCropDialog(bitmap, callback)
                }
            )

            pageRecyclerView = RecyclerView(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                adapter = informationAdapterView.createAdapter()
            }
            addView(pageRecyclerView)
        }

        addView(homePage)
    }

    private fun showCropDialog(bitmap: Bitmap, callBack: (Bitmap) -> Unit = {}) {
        cropDialog.showCropDialog(bitmap, 0) { it ->
            it?.let {
                callBack(it)
                context.ShowToast("DONE")
            }
        }
        cropDialog.showAtLocation(mainView)
    }
}
