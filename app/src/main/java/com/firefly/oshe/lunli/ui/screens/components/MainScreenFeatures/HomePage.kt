package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.client.Client

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserImage
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.dp
import com.google.android.material.button.MaterialButton

// 用户个人主页
class HomePage(
    private val context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val userInformation: UserInformation,
    private val userImage: UserImage
) {

    private lateinit var mainView: LinearLayout
    private lateinit var client: Client

    private final var pageRecyclerView: RecyclerView? = null
    private final var pageAdapter: BaseUserHomeAdapter? = null

    private var homePage: LinearLayout? = null

    fun interface onSignOutListener {
        fun onSignOut()
    }

    private var signOutListener: onSignOutListener? = null

    fun setOnSignOutListener(listener: onSignOutListener) {
        this.signOutListener = listener
    }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )

            createUserHomePageView()
        }
        return mainView
    }

    private fun LinearLayout.createUserHomePageView() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL

            pageRecyclerView = RecyclerView(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                pageAdapter = UserHomeAdapter()
                adapter = pageAdapter
            }
            addView(pageRecyclerView)
        }

        addView(homePage)
    }

    private abstract inner class BaseUserHomeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // TODO:
    }

    private inner class UserHomeAdapter: BaseUserHomeAdapter() {
        private val information = mutableListOf<UserInformation>()

        init {
            information.addAll(listOf(
                UserInformation(
                    userData.userId,
                    userData.userName,
                    "DUMMY",
                    "DUMMY"
                )
            ))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8.dp, 0, 8.dp)
            }

            val buttonLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 8.dp
                }
            }

            val exitButton = createButton("退出登录") {
                signOutListener?.onSignOut()
            }.apply {
                layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                    marginEnd = 4.dp
                }
            }.also { buttonLayout.addView(it) }

            root.addView(buttonLayout)

            return object : RecyclerView.ViewHolder(root) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val rootView = holder.itemView
        }

        override fun getItemCount() = information.size
    }

    private fun createButton(text: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.red)
            )
            cornerRadius = 8.dp
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(WRAP_CONTENT, 48.dp)
        }
    }

}
