package com.jibo.apptoolkit.android.model.api

import android.os.Parcel
import android.os.Parcelable
import com.jibo.apptoolkit.protocol.api.BaseCertificates


/**
 * Created by calvinator on 1/26/18.
 */
internal class Certificates : BaseCertificates, Parcelable {


    constructor(cert: String) : super(cert) {}

    protected constructor(`in`: Parcel) : super() {
        `in`.writeString(cert)
        `in`.writeString(publicKey)
        `in`.writeString(privateKey)
        `in`.writeString(fingerprint)
        `in`.writeLong(created)
        `in`.writeString(ipAddress)
        `in`.writeString(p12)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(cert)
        parcel.writeString(publicKey)
        parcel.writeString(privateKey)
        parcel.writeString(fingerprint)
        parcel.writeLong(created)
        parcel.writeString(ipAddress)
        parcel.writeString(p12)
    }

    companion object {

        val CREATOR: Parcelable.Creator<Certificates> = object : Parcelable.Creator<Certificates> {
            override fun createFromParcel(`in`: Parcel): Certificates {
                return Certificates(`in`)
            }

            override fun newArray(size: Int): Array<Certificates?> {
                return arrayOfNulls(size)
            }
        }
    }

}
