package com.floppy.app

import android.app.Application
import com.floppy.app.data.FloppyRepository
import com.floppy.app.data.RepositoryFactory

class FloppyApplication : Application() {
    val repository: FloppyRepository by lazy { RepositoryFactory.create(this) }
}
