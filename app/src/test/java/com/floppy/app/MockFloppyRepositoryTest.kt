package com.floppy.app

import com.floppy.app.data.FallbackAudioLibrary
import com.floppy.app.data.MockFloppyRepository
import com.floppy.app.domain.AudioLibrary
import com.floppy.app.domain.ContentPreference
import com.floppy.app.domain.Feedback
import com.floppy.app.domain.GenerationStatus
import com.floppy.app.domain.RecommendationResult
import com.floppy.app.domain.SleepIssue
import com.floppy.app.domain.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockFloppyRepositoryTest {
    @Test
    fun defaultLibraryUsesBundledFallbackAudio() = runTest {
        val repository = MockFloppyRepository()
        val library = repository.library.first()

        assertEquals(FallbackAudioLibrary.streamUrl, library.recommended.first().streamUrl)
    }

    @Test
    fun emptyLibraryFallsBackToBundledAudio() {
        val library = FallbackAudioLibrary.withFallbackIfEmpty(
            AudioLibrary(recommended = emptyList(), uploads = emptyList(), history = emptyList())
        )

        assertEquals(FallbackAudioLibrary.streamUrl, library.recommended.first().streamUrl)
    }

    @Test
    fun profileStartsEmptyUntilSaved() = runTest {
        val repository = MockFloppyRepository()

        assertNull(repository.profile.first())

        val profile = UserProfile()
        repository.saveProfile(profile)

        assertEquals(profile, repository.profile.first())
    }

    @Test
    fun profileWithStoryAndRacingThoughtsReturnsReadyRecommendation() = runTest {
        val repository = MockFloppyRepository()
        val result = repository.recommend(UserProfile())

        assertTrue(result is RecommendationResult.Ready)
    }

    @Test
    fun generationTaskSucceedsAfterPolling() = runTest {
        val repository = MockFloppyRepository()
        val profile = UserProfile(
            sleepIssues = setOf(SleepIssue.LightSleep),
            contentPreferences = setOf(ContentPreference.Meditation)
        )
        val task = repository.createGenerationTask("calm meditation", profile)

        var latest = task
        repeat(4) {
            latest = repository.pollGenerationTask(task.id)
        }

        assertEquals(GenerationStatus.Success, latest.status)
        assertNotNull(latest.audio)
    }

    @Test
    fun feedbackResponseChangesCopyByRating() = runTest {
        val repository = MockFloppyRepository()

        val low = repository.submitFeedback(Feedback(audioId = "a", rating = 2))
        val high = repository.submitFeedback(Feedback(audioId = "a", rating = 5))

        assertTrue(low.message.contains("避开"))
        assertTrue(high.message.contains("记住"))
    }
}
