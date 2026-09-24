package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreRepository
import com.example.data.model.Booking
import com.example.data.model.Message
import com.example.data.model.Property
import com.example.data.model.Review
import com.example.data.model.User
import com.example.ui.screens.AiInspectorScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookingsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateDarker
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    data class PropertyDetail(val property: Property) : Screen()
    data class AiInspector(val initialTab: Int = 0) : Screen()
    data class Chat(val recipientId: String, val recipientName: String, val recipientPhone: String) : Screen()
}

enum class BottomTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map),
    BOOKINGS("Bookings", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                StayFinderApp()
            }
        }
    }
}

@Composable
fun StayFinderApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager.getInstance(context) }
    val firestoreRepository = remember { FirestoreRepository(context) }

    // Authentication State
    var currentUser by remember { mutableStateOf(firebaseManager.getAuth()?.currentUser) }
    var currentUserId by remember { mutableStateOf(firebaseManager.getAuth()?.currentUser?.uid ?: "") }

    // Navigation State
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var currentBottomTab by remember { mutableStateOf(BottomTab.HOME) }

    // Real-time properties state from Firestore
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isPropertiesLoading by remember { mutableStateOf(true) }
    var propertiesError by remember { mutableStateOf<String?>(null) }

    // Real-time user profile, favorites, bookings
    var userProfile by remember { mutableStateOf<User?>(null) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }

    // Selected property reviews
    var propertyReviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    // Active chat messages
    var activeChatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }

    // Listen to real-time properties from Firebase Firestore
    LaunchedEffect(Unit) {
        firestoreRepository.getPropertiesFlow().collect { result ->
            isPropertiesLoading = false
            if (result.isSuccess) {
                properties = result.getOrNull() ?: emptyList()
                propertiesError = null
            } else {
                propertiesError = result.exceptionOrNull()?.message
            }
        }
    }

    // Listen to user data once authenticated
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            launch {
                firestoreRepository.getUserFlow(currentUserId).collect {
                    userProfile = it
                }
            }
            launch {
                firestoreRepository.getFavoritesFlow(currentUserId).collect {
                    favorites = it
                }
            }
            launch {
                firestoreRepository.getBookingsFlow(currentUserId).collect {
                    bookings = it
                }
            }
        } else {
            userProfile = null
            favorites = emptySet()
            bookings = emptyList()
        }
    }

    // Update reviews when viewing a property
    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.PropertyDetail) {
            val propId = (currentScreen as Screen.PropertyDetail).property.id
            firestoreRepository.getReviewsFlow(propId).collect {
                propertyReviews = it
            }
        }
    }

    // Update chat messages when in chat screen
    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.Chat) {
            val chat = currentScreen as Screen.Chat
            val chatId = if (currentUserId < chat.recipientId) "${currentUserId}_${chat.recipientId}" else "${chat.recipientId}_${currentUserId}"
            firestoreRepository.getMessagesFlow(chatId).collect {
                activeChatMessages = it
            }
        }
    }

    // If user is not authenticated, show Phone Auth Screen (Entirely In-App)
    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                val user = firebaseManager.getAuth()?.currentUser
                currentUser = user
                currentUserId = user?.uid ?: ""
            }
        )
        return
    }

    // Main App with Scaffold
    Box(modifier = Modifier.fillMaxSize().background(SlateDarker)) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.Main -> {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = SlateCard,
                                contentColor = EmeraldPrimary
                            ) {
                                BottomTab.values().forEach { tab ->
                                    val isSelected = currentBottomTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentBottomTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = EmeraldLight,
                                            selectedTextColor = EmeraldLight,
                                            indicatorColor = SlateDark,
                                            unselectedIconColor = SlateTextMuted,
                                            unselectedTextColor = SlateTextMuted
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentBottomTab) {
                                BottomTab.HOME -> {
                                    HomeScreen(
                                        properties = properties,
                                        favorites = favorites,
                                        isLoading = isPropertiesLoading,
                                        errorMessage = propertiesError,
                                        onFavoriteToggle = { propId ->
                                            scope.launch {
                                                firestoreRepository.toggleFavorite(
                                                    currentUserId,
                                                    propId,
                                                    favorites.contains(propId)
                                                )
                                            }
                                        },
                                        onPropertyClick = { prop ->
                                            currentScreen = Screen.PropertyDetail(prop)
                                        },
                                        onSearchClick = { currentBottomTab = BottomTab.EXPLORE },
                                        onAiAssistantClick = { currentScreen = Screen.AiInspector(initialTab = 2) },
                                        onAiInspectorClick = { currentScreen = Screen.AiInspector(initialTab = 0) },
                                        onAiLeaseClick = { currentScreen = Screen.AiInspector(initialTab = 1) },
                                        onRetry = {
                                            isPropertiesLoading = true
                                            propertiesError = null
                                        }
                                    )
                                }

                                BottomTab.EXPLORE -> {
                                    ExploreScreen(
                                        properties = properties,
                                        favorites = favorites,
                                        onFavoriteToggle = { propId ->
                                            scope.launch {
                                                firestoreRepository.toggleFavorite(
                                                    currentUserId,
                                                    propId,
                                                    favorites.contains(propId)
                                                )
                                            }
                                        },
                                        onPropertyClick = { prop ->
                                            currentScreen = Screen.PropertyDetail(prop)
                                        }
                                    )
                                }

                                BottomTab.MAP -> {
                                    MapScreen(
                                        properties = properties,
                                        favorites = favorites,
                                        onFavoriteToggle = { propId ->
                                            scope.launch {
                                                firestoreRepository.toggleFavorite(
                                                    currentUserId,
                                                    propId,
                                                    favorites.contains(propId)
                                                )
                                            }
                                        },
                                        onPropertyClick = { prop ->
                                            currentScreen = Screen.PropertyDetail(prop)
                                        }
                                    )
                                }

                                BottomTab.BOOKINGS -> {
                                    BookingsScreen(
                                        bookings = bookings,
                                        onCancelBooking = { bookingId ->
                                            scope.launch {
                                                firestoreRepository.cancelBooking(bookingId)
                                            }
                                        },
                                        onExploreClick = { currentBottomTab = BottomTab.EXPLORE }
                                    )
                                }

                                BottomTab.PROFILE -> {
                                    ProfileScreen(
                                        user = userProfile,
                                        favoriteCount = favorites.size,
                                        onUpdateProfile = { updated ->
                                            scope.launch {
                                                firestoreRepository.updateUserProfile(updated)
                                            }
                                        },
                                        onAiInspectorClick = { currentScreen = Screen.AiInspector(initialTab = 0) },
                                        onAiLeaseClick = { currentScreen = Screen.AiInspector(initialTab = 1) },
                                        onAiAssistantClick = { currentScreen = Screen.AiInspector(initialTab = 2) },
                                        onLogout = {
                                            firebaseManager.getAuth()?.signOut()
                                            currentUser = null
                                            currentUserId = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                is Screen.PropertyDetail -> {
                    val prop = screen.property
                    PropertyDetailScreen(
                        property = prop,
                        reviews = propertyReviews,
                        isFavorite = favorites.contains(prop.id),
                        onFavoriteToggle = {
                            scope.launch {
                                firestoreRepository.toggleFavorite(
                                    currentUserId,
                                    prop.id,
                                    favorites.contains(prop.id)
                                )
                            }
                        },
                        onBackClick = { currentScreen = Screen.Main },
                        onBookProperty = { booking ->
                            scope.launch {
                                val completeBooking = booking.copy(
                                    customerId = currentUserId,
                                    customerName = userProfile?.name ?: currentUser?.displayName ?: "Tenant",
                                    customerPhone = userProfile?.phone ?: currentUser?.phoneNumber ?: "",
                                    customerEmail = userProfile?.email ?: currentUser?.email ?: ""
                                )
                                firestoreRepository.createBooking(completeBooking)
                                currentBottomTab = BottomTab.BOOKINGS
                                currentScreen = Screen.Main
                            }
                        },
                        onAddReview = { review ->
                            scope.launch {
                                val completeReview = review.copy(
                                    userId = currentUserId,
                                    userName = userProfile?.name?.ifBlank { null } ?: "Verified Tenant"
                                )
                                firestoreRepository.addReview(completeReview)
                            }
                        },
                        onOpenChat = { ownerId, ownerName ->
                            currentScreen = Screen.Chat(
                                recipientId = ownerId.ifBlank { "owner_support" },
                                recipientName = ownerName.ifBlank { "Property Manager" },
                                recipientPhone = prop.ownerWhatsapp.ifBlank { prop.ownerPhone }
                            )
                        }
                    )
                }

                is Screen.AiInspector -> {
                    AiInspectorScreen(
                        initialTab = screen.initialTab,
                        realFirestoreProperties = properties,
                        onBackClick = { currentScreen = Screen.Main }
                    )
                }

                is Screen.Chat -> {
                    val chatId = if (currentUserId < screen.recipientId) "${currentUserId}_${screen.recipientId}" else "${screen.recipientId}_${currentUserId}"
                    ChatScreen(
                        currentUserId = currentUserId,
                        currentUserName = userProfile?.name?.ifBlank { null } ?: "Tenant",
                        chatId = chatId,
                        recipientName = screen.recipientName,
                        recipientPhone = screen.recipientPhone,
                        messages = activeChatMessages,
                        onSendMessage = { msg ->
                            scope.launch {
                                firestoreRepository.sendMessage(msg)
                            }
                        },
                        onBackClick = { currentScreen = Screen.Main }
                    )
                }
            }
        }
    }
}
