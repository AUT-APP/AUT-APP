package com.example.autapp.ui.notification

import com.example.autapp.data.firebase.FirebaseNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var fakeRepo: FakeNotificationRepository

    @Before
    fun setup() {
        fakeRepo = FakeNotificationRepository()
    }

    @Test
    fun `test viewing notifications`() = runTest {
        // Given
        val mockNotification = FirebaseNotification(
            title = "Reminder",
            text = "Your class starts soon",
            notificationType = "class",
            scheduledDeliveryTime = Date(System.currentTimeMillis())
        )
        fakeRepo = FakeNotificationRepository()
        fakeRepo.create(mockNotification)
        viewModel = NotificationViewModel(fakeRepo)
        viewModel.initialize("student123", isTeacher = false)

        // When
        viewModel.fetchNotificationData()

        // Then
        val notifications = viewModel.notifications.value
        assertEquals(1, notifications.size)
        assertEquals("Reminder", notifications[0].title)
    }

    @Test
    fun `test clearing all notifications`() = runTest {
        // Given
        val mockNotification = FirebaseNotification(
            title = "Exam Update",
            text = "Room changed",
            notificationType = "exam",
            scheduledDeliveryTime = Date(System.currentTimeMillis())
        )
        fakeRepo.create(mockNotification)
        viewModel = NotificationViewModel(fakeRepo)
        viewModel.initialize("student456", isTeacher = false)
        viewModel.fetchNotificationData()
        assert(viewModel.notifications.value.isNotEmpty())

        // When
        viewModel.clearAllNotifications()

        // Then
        assertTrue(viewModel.notifications.value.isEmpty())
    }

    @Test
    fun `test notifications sorted in reverse chronological order`() = runTest {
        // Given
        val older = FirebaseNotification(
            title = "Old Notice",
            text = "This is older",
            notificationType = "general",
            scheduledDeliveryTime = Date(1000L)
        )
        val newer = FirebaseNotification(
            title = "New Notice",
            text = "This is newer",
            notificationType = "general",
            scheduledDeliveryTime = Date(2000L)
        )
        fakeRepo.create(older)
        fakeRepo.create(newer)
        viewModel = NotificationViewModel(fakeRepo)
        viewModel.initialize("student789", isTeacher = false)

        // When
        viewModel.fetchNotificationData()

        // Then
        val notifications = viewModel.notifications.value
        assertEquals("New Notice", notifications.first().title)
    }
}