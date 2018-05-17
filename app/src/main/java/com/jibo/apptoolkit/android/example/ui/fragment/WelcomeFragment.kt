package com.jibo.apptoolkit.android.example.ui.fragment

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jibo.apptoolkit.android.JiboCommandControl
import com.jibo.apptoolkit.android.example.R
import com.jibo.apptoolkit.android.model.api.Robot
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.util.*

/**
 * A placeholder fragment containing a simple view.
 */
class WelcomeFragment : BaseFragment(), JiboCommandControl.OnAuthenticationListener {

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mRobots: ArrayList<Robot>? = null
    private val onAuthenticationListener = object : JiboCommandControl.OnAuthenticationListener {
        override fun onSuccess(robots: ArrayList<Robot>) {
            mRobots?.clear()
            mRobots?.addAll(robots)
            list.adapter.notifyDataSetChanged()

            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        override fun onError(throwable: Throwable) {
            log("API onError:" + throwable.localizedMessage)

            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        override fun onCancel() {
            log("API onCancel by user")

            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(activity)
        list.layoutManager = mLayoutManager
        list.setHasFixedSize(true)
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
                outRect.bottom = view.context.resources.getDimensionPixelSize(R.dimen.row_divider)
            }
        })

        mRobots = ArrayList()
        mAdapter = RobotsAdapter(mRobots ?: ArrayList())
        list.adapter = mAdapter

        button1.setOnClickListener { onConnectClick() }
        button2.setOnClickListener { onLogoutClick() }

    }

    //    @OnClick(android.R.id.button1)
    fun onConnectClick() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        JiboCommandControl.instance.signIn(activity as AppCompatActivity, onAuthenticationListener)
    }

    //    @OnClick(android.R.id.button2)
    fun onLogoutClick() {
        JiboCommandControl.instance.logOut()
    }

    private fun log(msg: String) {
        Log.d(BaseFragment.TAG, msg)
    }

    override fun onSuccess(robots: ArrayList<Robot>) {}

    override fun onError(throwable: Throwable) {

    }

    override fun onCancel() {

    }

    class RobotsAdapter(private val mRobots: ArrayList<Robot>) : RecyclerView.Adapter<RobotsAdapter.ViewHolder>() {

        class ViewHolder(var mTextView: TextView) : RecyclerView.ViewHolder(mTextView) {

            fun setData(robot: Robot) {
                itemView.tag = robot
                mTextView.text = robot.name
                itemView.setOnClickListener { view ->
                    val robot = view.tag as Robot
                    val activity = itemView.context as AppCompatActivity
                    val args = Bundle()
                    args.putParcelable(Robot::class.java.name, robot)

                    val fragment = ControlFragment()
                    fragment.arguments = args

                    activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment, fragment, ControlFragment::class.java.name)
                            .addToBackStack(ControlFragment::class.java.name)
                            .commit()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RobotsAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_robot, parent, false) as TextView
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setData(mRobots[position])

        }

        override fun getItemCount(): Int {
            return mRobots.size
        }
    }
}
