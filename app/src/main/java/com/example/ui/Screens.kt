package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoverLetter
import com.example.data.Education
import com.example.data.JsonHelpers
import com.example.data.Resume
import com.example.data.WorkExperience
import kotlinx.coroutines.launch

// Color assets from design system
val DeepNavy = Color(0xFF131B2E)
val SlateGray = Color(0xFF45464D)
val Emerald = Color(0xFF10B981)
val EmeraldLight = Color(0xFFD1FAE5)
val TealAccent = Color(0xFF14B8A6)
val SoftWhite = Color(0xFFF8F9FF)
val CardBorder = Color(0xFFC6C6CD)
val PrimaryEmerald = Color(0xFF006C49)

val AiGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
)

// Main App Navigation Container
@Composable
fun ResumeProApp(viewModel: ResumeViewModel) {
    var currentTab by remember { mutableStateOf("home") }
    val currentResume by viewModel.currentResume.collectAsState()
    val currentCoverLetter by viewModel.currentCoverLetter.collectAsState()

    // Handle back presses from sub-editors
    val onBackPressed = {
        if (currentResume != null) {
            viewModel.selectResume(null)
        } else if (currentCoverLetter != null) {
            viewModel.selectCoverLetter(null)
        } else {
            currentTab = "home"
        }
    }

    Scaffold(
        bottomBar = {
            if (currentResume == null && currentCoverLetter == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val tabs = listOf(
                        Triple("home", "Home", Icons.Outlined.Home),
                        Triple("builder", "Builder", Icons.Outlined.EditNote),
                        Triple("letters", "Letters", Icons.Outlined.Description),
                        Triple("gallery", "Gallery", Icons.Outlined.DashboardCustomize)
                    )
                    tabs.forEach { (tab, label, icon) ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = DeepNavy,
                                indicatorColor = DeepNavy,
                                unselectedIconColor = SlateGray,
                                unselectedTextColor = SlateGray
                            )
                        )
                    }
                }
            }
        },
        containerColor = SoftWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                currentResume != null -> {
                    ResumeEditorWorkspace(
                        resume = currentResume!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectResume(null) }
                    )
                }
                currentCoverLetter != null -> {
                    CoverLetterWorkspace(
                        letter = currentCoverLetter!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectCoverLetter(null) }
                    )
                }
                else -> {
                    when (currentTab) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onStartResume = {
                                viewModel.createNewResume()
                            },
                            onTabSwitch = { tab -> currentTab = tab }
                        )
                        "builder" -> BuilderTab(
                            viewModel = viewModel,
                            onStartResume = { viewModel.createNewResume() }
                        )
                        "letters" -> LettersTab(viewModel = viewModel)
                        "gallery" -> GalleryTab(
                            viewModel = viewModel,
                            onSelectTemplate = { templateId ->
                                viewModel.createNewResume("My $templateId Resume")
                                viewModel.updateCurrentResumeState { it.copy(templateId = templateId) }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// HOME TAB
// ----------------------------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: ResumeViewModel,
    onStartResume: () -> Unit,
    onTabSwitch: (String) -> Unit
) {
    val resumes by viewModel.allResumes.collectAsState()
    val coverLetters by viewModel.allCoverLetters.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Top Brand App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .border(1.dp, CardBorder.copy(alpha = 0.1f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Logo",
                        tint = DeepNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ResumePro AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepNavy
                    )
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Pro Upgrade: Success!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("upgrade_button")
                ) {
                    Text("Upgrade to Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Hero Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // New Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFE6F4EA))
                        .border(1.dp, Color(0xFFB7E1CD), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW: AI COVER LETTER GENERATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryEmerald
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Header text
                Text(
                    text = "AI-Powered Resumes for",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 38.sp,
                    color = DeepNavy
                )
                Text(
                    text = "Ambitious Professionals.",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 38.sp,
                    color = PrimaryEmerald
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Build a job-winning resume in minutes with our intelligent editor and expert-crafted templates. Designed to beat the ATS and impress human recruiters.",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = SlateGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Build / Templates buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartResume,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .background(AiGradient, RoundedCornerShape(12.dp))
                            .testTag("build_resume_hero_btn"),
                        contentPadding = PaddingValues()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build Your Resume", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = { onTabSwitch("gallery") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCE9FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .border(1.dp, CardBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Text("View Templates", fontWeight = FontWeight.Bold, color = DeepNavy)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Advertisement block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(Color(0xFFE5EEFF), RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ADVERTISEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // List of Active Resumes
        if (resumes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Resumes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    TextButton(onClick = { onTabSwitch("builder") }) {
                        Text("See All (${resumes.size})", color = PrimaryEmerald)
                    }
                }
            }

            items(resumes.take(3)) { resume ->
                ResumeCard(
                    resume = resume,
                    onClick = { viewModel.selectResume(resume) },
                    onDelete = { viewModel.deleteResume(resume) },
                    onDuplicate = { viewModel.duplicateResume(resume) }
                )
            }
        }

        // List of Active Cover Letters
        if (coverLetters.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Cover Letters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    TextButton(onClick = { onTabSwitch("letters") }) {
                        Text("See All (${coverLetters.size})", color = PrimaryEmerald)
                    }
                }
            }

            items(coverLetters.take(2)) { letter ->
                val matchingResume = resumes.find { it.id == letter.resumeId }
                CoverLetterListItem(
                    letter = letter,
                    resumeTitle = matchingResume?.title ?: "Personal Profile",
                    onClick = { viewModel.selectCoverLetter(letter) },
                    onDelete = { viewModel.deleteCoverLetter(letter) }
                )
            }
        }

        // Engineered for Modern Hiring (Bento Grid Header)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Engineered for Modern Hiring",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Built by experts to help you land interviews at top companies.",
                    fontSize = 13.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bento Card 1: AI Content Polishing
        item {
            BentoCard(
                icon = Icons.Default.AutoFixHigh,
                iconBg = Color(0xFFE6F4EA),
                iconColor = PrimaryEmerald,
                title = "AI Content Polishing",
                description = "Perfect every sentence with AI-driven suggestions that highlight your achievements using high-impact action verbs and industry-specific keywords.",
                chips = listOf("Action Oriented", "Keyword Optimized"),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Bento Card 2: Smart Templates
        item {
            BentoCard(
                icon = Icons.Default.Style,
                iconBg = Color(0xFFE5EEFF),
                iconColor = DeepNavy,
                title = "Smart Templates",
                description = "Choose from 4 professional styles designed specifically to be parsed flawlessly by ATS systems.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                darkStyle = true
            )
        }

        // Bento Card 3: Cover Letter Magic
        item {
            BentoCard(
                icon = Icons.Default.Description,
                iconBg = Color(0xFFF3E8FF),
                iconColor = Color(0xFF7C3AED),
                title = "Cover Letter Magic",
                description = "Generate matching cover letters instantly, tailored to specific job descriptions using your resume data.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Bento Card 4: Real-time Preview Mockup
        item {
            BentoCard(
                icon = Icons.Default.Visibility,
                iconBg = Color(0xFFFFF3CD),
                iconColor = Color(0xFFD97706),
                title = "Real-time Preview",
                description = "See your resume take shape as you type. Professional layout adjustments happen automatically as you add content.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                extraContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        // Drawing a tiny mock document layout
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).background(DeepNavy, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("AC", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(DeepNavy.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.width(40.dp).height(6.dp).background(SlateGray.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PrimaryEmerald.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.width(100.dp).height(8.dp).background(DeepNavy.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(SlateGray.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).background(SlateGray.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryEmerald)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("PREVIEW LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Trust section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy)
                    .padding(vertical = 36.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Trusted by Professionals at",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val logos = listOf("TECHCORP", "GLOBALFIN", "HEALTHLINE", "NEXTGEN")
                    logos.forEach { logo ->
                        Text(
                            text = logo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// BUILDER TAB
// ----------------------------------------------------------------------
@Composable
fun BuilderTab(viewModel: ResumeViewModel, onStartResume: () -> Unit) {
    val resumes by viewModel.allResumes.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Resume Builder", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("Manage and edit your CV profiles", fontSize = 13.sp, color = SlateGray)
            }
            IconButton(
                onClick = onStartResume,
                modifier = Modifier
                    .background(PrimaryEmerald, CircleShape)
                    .size(40.dp)
                    .testTag("create_resume_icon_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }

        if (resumes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = SlateGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Resumes Created Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Click the button below to start creating your first ATS-optimized resume.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onStartResume,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Resume")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resumes) { resume ->
                    ResumeCard(
                        resume = resume,
                        onClick = { viewModel.selectResume(resume) },
                        onDelete = { viewModel.deleteResume(resume) },
                        onDuplicate = { viewModel.duplicateResume(resume) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// LETTERS TAB
// ----------------------------------------------------------------------
@Composable
fun LettersTab(viewModel: ResumeViewModel) {
    val coverLetters by viewModel.allCoverLetters.collectAsState()
    val resumes by viewModel.allResumes.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cover Letters", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                Text("AI-tailored cover letters for specific job postings", fontSize = 13.sp, color = SlateGray)
            }
            IconButton(
                onClick = {
                    if (resumes.isNotEmpty()) {
                        viewModel.createNewCoverLetter(resumes.first().id)
                    } else {
                        viewModel.createNewCoverLetter(0)
                    }
                },
                modifier = Modifier
                    .background(PrimaryEmerald, CircleShape)
                    .size(40.dp)
                    .testTag("create_letter_icon_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }

        if (coverLetters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = SlateGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Cover Letters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Synthesize matching cover letters beautifully using your resume profiles.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (resumes.isNotEmpty()) {
                                viewModel.createNewCoverLetter(resumes.first().id)
                            } else {
                                viewModel.createNewCoverLetter(0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Write New Letter")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(coverLetters) { letter ->
                    val matchingResume = resumes.find { it.id == letter.resumeId }
                    CoverLetterListItem(
                        letter = letter,
                        resumeTitle = matchingResume?.title ?: "Personal Profile",
                        onClick = { viewModel.selectCoverLetter(letter) },
                        onDelete = { viewModel.deleteCoverLetter(letter) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// GALLERY TAB
// ----------------------------------------------------------------------
@Composable
fun GalleryTab(viewModel: ResumeViewModel, onSelectTemplate: (String) -> Unit) {
    val templates = listOf(
        TemplateItem("executive", "Executive Precision", "High-trust deep navy headers styled with formal document spacing, ideal for engineering and business leaders.", PrimaryEmerald),
        TemplateItem("emerald", "Emerald Modern", "Sleek split layout featuring a side accent column and custom chips, perfect for modern tech and creative roles.", Color(0xFF10B981)),
        TemplateItem("cosmic", "Cosmic Slate", "Balanced, centered minimalism with light-slate styling and dotted contact separators for an elegant modern appeal.", Color(0xFF45464D)),
        TemplateItem("stark", "Stark Minimalist", "Razor-sharp, high-density monochrome layout prioritizing content clarity and structural grid borders.", Color(0xFF111827))
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text("Smart Templates", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
            Text("Engineered for flawless ATS parsing and recruitment scoring", fontSize = 13.sp, color = SlateGray)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates) { tmpl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTemplate(tmpl.id) }
                        .testTag("template_card_${tmpl.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(tmpl.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tmpl.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(tmpl.description, fontSize = 13.sp, color = SlateGray, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onSelectTemplate(tmpl.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = tmpl.color),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Use This Style")
                        }
                    }
                }
            }
        }
    }
}

data class TemplateItem(val id: String, val name: String, val description: String, val color: Color)

// ----------------------------------------------------------------------
// BENTO CARD / HELPER COMPONENTS
// ----------------------------------------------------------------------
@Composable
fun BentoCard(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    description: String,
    chips: List<String> = emptyList(),
    extraContent: (@Composable () -> Unit)? = null,
    darkStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (darkStyle) DeepNavy else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (darkStyle) Color.White.copy(alpha = 0.15f) else iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (darkStyle) Color(0xFF6CF8BB) else iconColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (darkStyle) Color.White else DeepNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = if (darkStyle) Color.White.copy(alpha = 0.7f) else SlateGray
            )

            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(PrimaryEmerald.copy(alpha = 0.1f))
                                .border(1.dp, PrimaryEmerald.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(chip, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                        }
                    }
                }
            }

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                extraContent()
            }
        }
    }
}

@Composable
fun ResumeCard(
    resume: Resume,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("resume_card_${resume.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE5EEFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Article, contentDescription = null, tint = DeepNavy)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resume.title.ifBlank { "Untitled Profile" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = when (resume.templateId) {
                            "executive" -> DeepNavy
                            "emerald" -> Color(0xFF10B981)
                            "cosmic" -> SlateGray
                            else -> Color.Black
                        },
                        modifier = Modifier.size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Template: ${resume.templateId.replaceFirstChar { it.uppercaseChar() }}",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
            }

            Row {
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = SlateGray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CoverLetterListItem(
    letter: CoverLetter,
    resumeTitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("cover_letter_card_${letter.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF3E8FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF7C3AED))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (letter.companyName.isNotBlank() && letter.jobTitle.isNotBlank()) {
                        "${letter.jobTitle} - ${letter.companyName}"
                    } else {
                        "Tailored Cover Letter"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matched to: $resumeTitle",
                    fontSize = 12.sp,
                    color = SlateGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ----------------------------------------------------------------------
// RESUME BUILDER WORKSPACE
// ----------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorWorkspace(
    resume: Resume,
    viewModel: ResumeViewModel,
    onBack: () -> Unit
) {
    var activeStep by remember { mutableStateOf("contact") }
    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusMsg by viewModel.aiStatusMessage.collectAsState()
    val context = LocalContext.current

    // Contact State
    var title by remember { mutableStateOf(resume.title) }
    var fullName by remember { mutableStateOf(resume.fullName) }
    var email by remember { mutableStateOf(resume.email) }
    var phone by remember { mutableStateOf(resume.phone) }
    var website by remember { mutableStateOf(resume.website) }
    var summary by remember { mutableStateOf(resume.summary) }
    var skills by remember { mutableStateOf(resume.skills) }

    // Lists (Experience, Education) state
    var experiences by remember { mutableStateOf(JsonHelpers.parseExperiences(resume.experienceJson)) }
    var educations by remember { mutableStateOf(JsonHelpers.parseEducation(resume.educationJson)) }

    // Active Template selection
    var templateId by remember { mutableStateOf(resume.templateId) }

    // Dialog State
    var showExpDialog by remember { mutableStateOf(false) }
    var editingExperience by remember { mutableStateOf<WorkExperience?>(null) }
    var expTitle by remember { mutableStateOf("") }
    var expCompany by remember { mutableStateOf("") }
    var expStart by remember { mutableStateOf("") }
    var expEnd by remember { mutableStateOf("") }
    var expDesc by remember { mutableStateOf("") }

    var showEduDialog by remember { mutableStateOf(false) }
    var editingEducation by remember { mutableStateOf<Education?>(null) }
    var eduDegree by remember { mutableStateOf("") }
    var eduSchool by remember { mutableStateOf("") }
    var eduGrad by remember { mutableStateOf("") }

    // Clear statuses on start
    LaunchedEffect(Unit) {
        viewModel.clearStatusMessage()
    }

    // Auto-save changes locally
    val saveChanges = {
        viewModel.updateCurrentResumeState { current ->
            current.copy(
                title = title,
                fullName = fullName,
                email = email,
                phone = phone,
                website = website,
                summary = summary,
                skills = skills,
                experienceJson = JsonHelpers.serializeExperiences(experiences),
                educationJson = JsonHelpers.serializeEducation(educations),
                templateId = templateId
            )
        }
        viewModel.saveCurrentResume()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = DeepNavy,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        saveChanges()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            saveChanges()
                            Toast.makeText(context, "Resume Saved Successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal Step Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, CardBorder.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val steps = listOf(
                    "contact" to "Contact",
                    "exp" to "Experience",
                    "edu" to "Education",
                    "preview" to "Preview"
                )
                steps.forEach { (step, label) ->
                    val active = activeStep == step
                    TextButton(
                        onClick = {
                            saveChanges()
                            activeStep = step
                        },
                        modifier = Modifier.testTag("step_tab_$step")
                    ) {
                        Text(
                            text = label,
                            color = if (active) PrimaryEmerald else SlateGray,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // AI Operation Status Notification banner
            AnimatedVisibility(visible = statusMsg != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (statusMsg?.contains("Error") == true) Color(0xFFFFDAD6) else Color(0xFFE6F4EA))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMsg ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (statusMsg?.contains("Error") == true) Color(0xFFBA1A1A) else Color(0xFF002113),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearStatusMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SoftWhite)
            ) {
                when (activeStep) {
                    "contact" -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("full_name_input"),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Phone Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = website,
                                    onValueChange = { website = it },
                                    label = { Text("Website/Portfolio") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Professional Summary with AI Polishing Button!
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = summary,
                                        onValueChange = { summary = it },
                                        label = { Text("Professional Summary") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .testTag("summary_input"),
                                        maxLines = 6
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.polishSummary(summary) { polished ->
                                                summary = polished
                                                saveChanges()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(AiGradient, RoundedCornerShape(8.dp))
                                            .testTag("ai_polish_summary_btn"),
                                        enabled = !isGenerating && summary.isNotBlank()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isGenerating) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                            } else {
                                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI Content Polishing (Summary)", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Skills Field
                            item {
                                OutlinedTextField(
                                    value = skills,
                                    onValueChange = { skills = it },
                                    label = { Text("Skills (Comma-separated)") },
                                    placeholder = { Text("e.g. Kotlin, Jetpack Compose, Room DB") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    "exp" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Work History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                                Button(
                                    onClick = {
                                        editingExperience = null
                                        expTitle = ""
                                        expCompany = ""
                                        expStart = ""
                                        expEnd = ""
                                        expDesc = ""
                                        showExpDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("add_experience_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Job")
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(experiences) { exp ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(exp.jobTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavy)
                                                    Text("${exp.company} (${exp.startDate} - ${exp.endDate})", fontSize = 12.sp, color = SlateGray)
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        editingExperience = exp
                                                        expTitle = exp.jobTitle
                                                        expCompany = exp.company
                                                        expStart = exp.startDate
                                                        expEnd = exp.endDate
                                                        expDesc = exp.description
                                                        showExpDialog = true
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SlateGray)
                                                    }
                                                    IconButton(onClick = {
                                                        experiences = experiences.filter { it.id != exp.id }
                                                        saveChanges()
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                            if (exp.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    exp.description,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = SlateGray,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "edu" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Education Background", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                                Button(
                                    onClick = {
                                        editingEducation = null
                                        eduDegree = ""
                                        eduSchool = ""
                                        eduGrad = ""
                                        showEduDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add School")
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(educations) { edu ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavy)
                                                    Text(edu.school, fontSize = 13.sp, color = SlateGray)
                                                    Text("Graduated: ${edu.gradDate}", fontSize = 11.sp, color = SlateGray)
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        editingEducation = edu
                                                        eduDegree = edu.degree
                                                        eduSchool = edu.school
                                                        eduGrad = edu.gradDate
                                                        showEduDialog = true
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SlateGray)
                                                    }
                                                    IconButton(onClick = {
                                                        educations = educations.filter { it.id != edu.id }
                                                        saveChanges()
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "preview" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Style Picker Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Style:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val styles = listOf("executive", "emerald", "cosmic", "stark")
                                    styles.forEach { id ->
                                        val selected = templateId == id
                                        val styleColor = when (id) {
                                            "executive" -> DeepNavy
                                            "emerald" -> Color(0xFF10B981)
                                            "cosmic" -> SlateGray
                                            else -> Color.Black
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(styleColor, CircleShape)
                                                .border(
                                                    2.dp,
                                                    if (selected) PrimaryEmerald else Color.Transparent,
                                                    CircleShape
                                                )
                                                .clickable {
                                                    templateId = id
                                                    saveChanges()
                                                }
                                                .testTag("style_indicator_$id")
                                        )
                                    }
                                }
                            }

                            // Mock rendered Resume
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxSize(),
                                    border = BorderStroke(1.dp, CardBorder),
                                    shape = RoundedCornerShape(4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    ResumeDocumentView(
                                        name = fullName,
                                        email = email,
                                        phone = phone,
                                        web = website,
                                        summary = summary,
                                        skills = skills,
                                        experiences = experiences,
                                        educations = educations,
                                        styleId = templateId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Work Experience Add/Edit Dialog
    if (showExpDialog) {
        AlertDialog(
            onDismissRequest = { showExpDialog = false },
            title = { Text(if (editingExperience == null) "Add Work History" else "Edit Work History") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = expTitle,
                            onValueChange = { expTitle = it },
                            label = { Text("Job Title") },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_exp_title")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = expCompany,
                            onValueChange = { expCompany = it },
                            label = { Text("Company Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = expStart,
                                onValueChange = { expStart = it },
                                label = { Text("Start Date") },
                                placeholder = { Text("YYYY-MM") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expEnd,
                                onValueChange = { expEnd = it },
                                label = { Text("End Date") },
                                placeholder = { Text("Present") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = expDesc,
                            onValueChange = { expDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                viewModel.polishExperienceBullet(expDesc) { polished ->
                                    expDesc = polished
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AiGradient, RoundedCornerShape(8.dp)),
                            enabled = !isGenerating && expDesc.isNotBlank()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isGenerating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Polish Description", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (expTitle.isNotBlank() && expCompany.isNotBlank()) {
                            if (editingExperience == null) {
                                experiences = experiences + WorkExperience(
                                    jobTitle = expTitle,
                                    company = expCompany,
                                    startDate = expStart,
                                    endDate = expEnd,
                                    description = expDesc
                                )
                            } else {
                                experiences = experiences.map {
                                    if (it.id == editingExperience!!.id) {
                                        it.copy(
                                            jobTitle = expTitle,
                                            company = expCompany,
                                            startDate = expStart,
                                            endDate = expEnd,
                                            description = expDesc
                                        )
                                    } else it
                                }
                            }
                            saveChanges()
                            showExpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Education Add/Edit Dialog
    if (showEduDialog) {
        AlertDialog(
            onDismissRequest = { showEduDialog = false },
            title = { Text(if (editingEducation == null) "Add Education" else "Edit Education") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = eduDegree,
                        onValueChange = { eduDegree = it },
                        label = { Text("Degree / Certificate") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eduSchool,
                        onValueChange = { eduSchool = it },
                        label = { Text("School / Institution") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eduGrad,
                        onValueChange = { eduGrad = it },
                        label = { Text("Graduation Date") },
                        placeholder = { Text("e.g. June 2019") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eduDegree.isNotBlank() && eduSchool.isNotBlank()) {
                            if (editingEducation == null) {
                                educations = educations + Education(
                                    degree = eduDegree,
                                    school = eduSchool,
                                    gradDate = eduGrad
                                )
                            } else {
                                educations = educations.map {
                                    if (it.id == editingEducation!!.id) {
                                        it.copy(
                                            degree = eduDegree,
                                            school = eduSchool,
                                            gradDate = eduGrad
                                        )
                                    } else it
                                }
                            }
                            saveChanges()
                            showEduDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEduDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------
// RESUME RENDER PREVIEW IMPLEMENTATIONS
// ----------------------------------------------------------------------
@Composable
fun ResumeDocumentView(
    name: String,
    email: String,
    phone: String,
    web: String,
    summary: String,
    skills: String,
    experiences: List<WorkExperience>,
    educations: List<Education>,
    styleId: String
) {
    val cleanName = name.ifBlank { "Alex Chun" }
    val cleanEmail = email.ifBlank { "alex.chun@techcorp.com" }
    val cleanPhone = phone.ifBlank { "+1 (555) 019-2834" }
    val cleanWeb = web.ifBlank { "github.com/alexchun" }
    val cleanSummary = summary.ifBlank { "Professional summary outlining top skillsets and achievements." }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(14.dp)
    ) {
        when (styleId) {
            "executive" -> {
                // Style 1: Executive Precision (Deep Navy Headers, Left Aligned)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(cleanName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$cleanEmail  |  $cleanPhone  |  $cleanWeb", fontSize = 9.sp, color = SlateGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DeepNavy))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("PROFESSIONAL SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cleanSummary, fontSize = 10.sp, lineHeight = 15.sp, color = Color.Black)
                }

                if (skills.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("CORE SKILLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(skills, fontSize = 10.sp, color = Color.Black, lineHeight = 14.sp)
                    }
                }

                if (experiences.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("EXPERIENCE HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, letterSpacing = 1.sp)
                    }

                    items(experiences) { exp ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exp.jobTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("${exp.startDate} - ${exp.endDate}", fontSize = 9.sp, color = SlateGray)
                        }
                        Text(exp.company, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateGray)
                        if (exp.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• ${exp.description}", fontSize = 9.5.sp, lineHeight = 14.sp, color = Color.DarkGray)
                        }
                    }
                }

                if (educations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("EDUCATION BACKGROUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, letterSpacing = 1.sp)
                    }

                    items(educations) { edu ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(edu.degree, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(edu.gradDate, fontSize = 9.sp, color = SlateGray)
                        }
                        Text(edu.school, fontSize = 9.sp, color = SlateGray)
                    }
                }
            }

            "emerald" -> {
                // Style 2: Emerald Modern (Modern 2-column style layout)
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left main column
                        Column(modifier = Modifier.weight(0.65f).padding(end = 8.dp)) {
                            Text(cleanName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(30.dp).height(3.dp).background(Color(0xFF10B981)))

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C49))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cleanSummary, fontSize = 9.sp, lineHeight = 13.sp, color = Color.Black)

                            if (experiences.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Experience", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C49))
                                experiences.forEach { exp ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(exp.jobTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("${exp.company} | ${exp.startDate} - ${exp.endDate}", fontSize = 8.sp, color = SlateGray)
                                    if (exp.description.isNotBlank()) {
                                        Text(exp.description, fontSize = 8.5.sp, lineHeight = 12.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }

                        // Right sidebar column
                        Column(
                            modifier = Modifier
                                .weight(0.35f)
                                .background(Color(0xFFEFFDF5), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text("Contact", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C49))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("✉ $cleanEmail", fontSize = 8.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("☎ $cleanPhone", fontSize = 8.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("🔗 $cleanWeb", fontSize = 8.sp, color = Color.Black)

                            if (skills.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Skills", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C49))
                                Spacer(modifier = Modifier.height(6.dp))
                                skills.split(",").forEach { skill ->
                                    val trimmed = skill.trim()
                                    if (trimmed.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 2.dp)
                                                .background(Color.White, RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color(0xFF6CF8BB), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(trimmed, fontSize = 7.5.sp, color = Color(0xFF00714D), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            if (educations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Education", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C49))
                                educations.forEach { edu ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(edu.degree, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(edu.school, fontSize = 7.5.sp, color = SlateGray)
                                }
                            }
                        }
                    }
                }
            }

            "cosmic" -> {
                // Style 3: Cosmic Slate (Centered layout)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(cleanName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$cleanEmail  •  $cleanPhone  •  $cleanWeb", fontSize = 8.5.sp, color = SlateGray)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateGray, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SlateGray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cleanSummary, fontSize = 9.5.sp, lineHeight = 14.sp, color = Color.Black)
                    }
                }

                if (skills.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("CORE EXPERTISE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateGray, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SlateGray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(skills, fontSize = 9.5.sp, color = Color.Black, lineHeight = 13.sp)
                    }
                }

                if (experiences.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("CHRONOLOGICAL EXPERIENCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateGray, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SlateGray))
                    }

                    items(experiences) { exp ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exp.jobTitle, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("${exp.startDate} - ${exp.endDate}", fontSize = 8.5.sp, color = SlateGray)
                        }
                        Text(exp.company, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = SlateGray)
                        if (exp.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(exp.description, fontSize = 9.sp, lineHeight = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                if (educations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ACADEMIC HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SlateGray, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SlateGray))
                    }

                    items(educations) { edu ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(edu.degree, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(edu.gradDate, fontSize = 8.5.sp, color = SlateGray)
                        }
                        Text(edu.school, fontSize = 8.5.sp, color = SlateGray)
                    }
                }
            }

            else -> {
                // Style 4: Stark Minimalist (Razor-sharp grid monochrome layout)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black)
                            .padding(10.dp)
                    ) {
                        Text(cleanName.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("CONTACT: $cleanEmail | $cleanPhone | $cleanWeb", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("OBJECTIVE SUMMARY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cleanSummary, fontSize = 9.sp, lineHeight = 13.sp, color = Color.Black)
                }

                if (skills.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("CAPABILITIES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(skills.uppercase(), fontSize = 8.5.sp, color = Color.Black)
                    }
                }

                if (experiences.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("CHRONOLOGICAL HISTORIES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                    }

                    items(experiences) { exp ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.jobTitle.uppercase()} - ${exp.company.uppercase()}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            Text("${exp.startDate} / ${exp.endDate}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        if (exp.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(exp.description, fontSize = 8.5.sp, lineHeight = 12.sp, color = Color.Black)
                        }
                    }
                }

                if (educations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ACADEMICS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                    }

                    items(educations) { edu ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${edu.degree.uppercase()} at ${edu.school.uppercase()}", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(edu.gradDate, fontSize = 8.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// COVER LETTER WORKSPACE
// ----------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterWorkspace(
    letter: CoverLetter,
    viewModel: ResumeViewModel,
    onBack: () -> Unit
) {
    val resumes by viewModel.allResumes.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusMsg by viewModel.aiStatusMessage.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Local letter states
    var jobTitle by remember { mutableStateOf(letter.jobTitle) }
    var companyName by remember { mutableStateOf(letter.companyName) }
    var jobDescription by remember { mutableStateOf(letter.jobDescription) }
    var recipientName by remember { mutableStateOf(letter.recipientName) }
    var content by remember { mutableStateOf(letter.content) }
    var selectedResumeId by remember { mutableStateOf(if (letter.resumeId == 0 && resumes.isNotEmpty()) resumes.first().id else letter.resumeId) }

    val selectedResume = resumes.find { it.id == selectedResumeId }

    // Save active changes local handler
    val saveLetterChanges = {
        viewModel.updateCurrentCoverLetterState { current ->
            current.copy(
                resumeId = selectedResumeId,
                jobTitle = jobTitle,
                companyName = companyName,
                jobDescription = jobDescription,
                recipientName = recipientName,
                content = content
            )
        }
        viewModel.saveCurrentCoverLetter()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cover Letter Workspace", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepNavy) },
                navigationIcon = {
                    IconButton(onClick = {
                        saveLetterChanges()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            saveLetterChanges()
                            Toast.makeText(context, "Cover Letter Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status notifier banner
            AnimatedVisibility(visible = statusMsg != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (statusMsg?.contains("Error") == true) Color(0xFFFFDAD6) else Color(0xFFE6F4EA))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMsg ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (statusMsg?.contains("Error") == true) Color(0xFFBA1A1A) else Color(0xFF002113),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearStatusMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // Selector of source Resume Profile
                item {
                    Column {
                        Text("Base Resume Profile:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (resumes.isEmpty()) {
                            Text("No resumes found. Please create a resume first so AI can use your skills/experience.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                resumes.forEach { res ->
                                    val isSelected = selectedResumeId == res.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryEmerald.copy(alpha = 0.15f) else Color.White)
                                            .border(
                                                1.dp,
                                                if (isSelected) PrimaryEmerald else CardBorder.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedResumeId = res.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("resume_select_${res.id}")
                                    ) {
                                        Text(res.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryEmerald else DeepNavy)
                                    }
                                }
                            }
                        }
                    }
                }

                // Inputs
                item {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Target Job Title") },
                        modifier = Modifier.fillMaxWidth().testTag("job_title_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Hiring Manager/Recipient Name") },
                        placeholder = { Text("e.g. Samantha Sterling, Hiring Director") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Job Posting Description / Highlights") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }

                // AI Generator Action Trigger
                item {
                    Button(
                        onClick = {
                            if (selectedResume != null) {
                                viewModel.generateCoverLetter(
                                    resume = selectedResume,
                                    jobTitle = jobTitle,
                                    companyName = companyName,
                                    jobDescription = jobDescription,
                                    recipientName = recipientName
                                ) { generatedLetter ->
                                    content = generatedLetter
                                    saveLetterChanges()
                                }
                            } else {
                                Toast.makeText(context, "Please create/select a resume profile first.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(AiGradient, RoundedCornerShape(10.dp))
                            .testTag("ai_generate_letter_btn"),
                        enabled = !isGenerating && jobTitle.isNotBlank() && companyName.isNotBlank()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Cover Letter Magic", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                // Rendered output Letter Document Workspace
                if (content.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated Draft Content:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(content))
                                    Toast.makeText(context, "Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_letter_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Content")
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = {
                                content = it
                                saveLetterChanges()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .testTag("letter_content_input"),
                            maxLines = 100,
                            textStyle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, color = Color.Black)
                        )
                    }
                }
            }
        }
    }
}
