package com.jibo.apptoolkit.android.model.api

import android.os.Parcel
import android.os.Parcelable
import com.jibo.apptoolkit.protocol.api.BaseRobot

/*
 * Created by calvinator on 1/26/18.
 */

/** Class for robot info  */
open class Robot : BaseRobot, Parcelable {


    /**
     * Information about the authenticated robot
     * @param id Unique ID of the robot
     * @param name Loop name. Usually `OwnerFirstName's Jibo`
     * @param robotName `My-Friendly-Robot-Name`, found on the underside of the robot's base
     */
    constructor(id: String, name: String, robotName: String) : super(id, name, robotName) {}

    internal constructor(robot: BaseRobot) : super(robot.id, robot.name, robot.robotName) {}

    constructor(parcel: Parcel) : super(parcel.readString(), parcel.readString(), parcel.readString()) {}

    override fun toString(): String {
        return "$id:$name:$robotName"
    }


    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(robotName)
    }

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<Robot> = object : Parcelable.Creator<Robot> {
            override fun createFromParcel(parcel: Parcel): Robot {
                return Robot(parcel)
            }

            override fun newArray(size: Int): Array<Robot?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * Get a list of all robots associated with the user’s authenticated account.
         * It is suggested that you prompt users to select which robot they would
         * like to connect to use your app in the event that they own multiple robots.
         * @return robots Robots for whom this user is the owner.
         */
        fun getRobot(baseRobots: List<BaseRobot>): ArrayList<Robot> {
            val robots = ArrayList<Robot>()
            for (robot in baseRobots) {
                if (robot.robotName != null) {
                    robots.add(Robot(robot))
                }
            }
            return robots
        }
    }

}
