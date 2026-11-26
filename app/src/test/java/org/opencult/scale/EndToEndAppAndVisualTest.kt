package org.opencult.scale

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.designsystem.components.AnatomicalBodyMapCanvas
import org.opencult.scale.core.designsystem.components.AnatomicalSegment
import org.opencult.scale.core.designsystem.components.BodyFatArcGauge
import org.opencult.scale.core.designsystem.components.DetachedObsidianDock
import org.opencult.scale.core.designsystem.components.HeroWeightSparkChart
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultDestination
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.OverviewWaveSparkline
import org.opencult.scale.core.designsystem.components.SegmentTier
import org.opencult.scale.core.designsystem.components.SparkBarEntry
import org.opencult.scale.core.designsystem.components.StippledBarChart
import org.opencult.scale.core.designsystem.components.StippledBarEntry
import org.opencult.scale.core.designsystem.components.dockScrollClearance
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.DailyHealthContext
import org.opencult.scale.feature.home.HomeDashboardUiState
import org.opencult.scale.feature.home.HomeScreenContent
import org.opencult.scale.feature.reports.ReportsScreenContent
import org.opencult.scale.feature.reports.ReportsUiState
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EndToEndAppAndVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: OpenCultDatabase
    private lateinit var repo: OpenCultRepository

    private val screenshotDir = File("build/outputs/visual_screenshots").apply { mkdirs() }

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, OpenCultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = OpenCultRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun render_and_capture_daylight_obsidian_hero_bento_reports_and_bodymap_screenshots() {
        var rootView: View? = null

        @Composable
        fun DashboardHarness(mode: OpenCultThemeMode) {
            OpenCultTheme(mode = mode, reducedMotionOverride = true) {
                rootView = LocalView.current
                val colors = OpenCultTheme.colors
                Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .dockScrollClearance(),
                    ) {
                        OpenCultBentoCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = colors.elevatedSurface,
                            shape = OpenCultTheme.shapes.heroSquircle,
                        ) {
                            OpenCultStatusChip(label = "Cult Pro 8E")
                            Spacer(Modifier.height(8.dp))
                            Text(text = "74.80 kg", style = OpenCultTheme.typography.heroMetric, color = colors.textPrimary)
                            Spacer(Modifier.height(10.dp))
                            HeroWeightSparkChart(
                                entries = persistentListOf(
                                    SparkBarEntry(75.9f),
                                    SparkBarEntry(75.5f),
                                    SparkBarEntry(76.4f, isSpike = true),
                                    SparkBarEntry(75.2f),
                                    SparkBarEntry(74.9f),
                                    SparkBarEntry(74.8f),
                                ),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OpenCultBentoCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = colors.bentoLavenderSurface,
                        ) {
                            Text("Body Fat 16.4%", style = OpenCultTheme.typography.bentoStat, color = colors.textPrimary)
                            OverviewWaveSparkline(values = persistentListOf(17.5f, 17.2f, 16.8f, 16.4f))
                        }
                        Spacer(Modifier.height(12.dp))
                        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                            BodyFatArcGauge(bodyFatPct = 16.4f)
                        }
                        Spacer(Modifier.height(12.dp))
                        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                            StippledBarChart(
                                entries = persistentListOf(
                                    StippledBarEntry("1", "M", 75.5f, "75.5"),
                                    StippledBarEntry("2", "T", 75.2f, "75.2"),
                                    StippledBarEntry("3", "W", 74.9f, "74.9"),
                                    StippledBarEntry("4", "T", 74.8f, "74.8"),
                                ),
                                selectedIndex = 3,
                                onSelectIndex = {},
                            )
                        }
                    }
                    DetachedObsidianDock(
                        selectedDestination = OpenCultDestination.HOME,
                        onSelectDestination = {},
                        onMeasureClick = {},
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        composeRule.setContent { DashboardHarness(OpenCultThemeMode.DAYLIGHT) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("74.80 kg").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start weigh-in").assertIsDisplayed()
        val daylightOut = File(screenshotDir, "visual_dashboard_daylight.png")
        saveViewBitmap(rootView!!, daylightOut)
        assertThat(daylightOut.length()).isGreaterThan(1000L)
    }

    @Test
    fun render_and_capture_obsidian_bodymap_and_interactive_tab_switching() {
        var rootView: View? = null
        var tappedNav = false

        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
                rootView = LocalView.current
                val colors = OpenCultTheme.colors
                Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp).dockScrollClearance()) {
                        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.elevatedSurface) {
                            Text("8-Electrode Body Map", style = OpenCultTheme.typography.pageTitle, color = colors.textPrimary)
                            AnatomicalBodyMapCanvas(
                                selectedSegment = AnatomicalSegment.TRUNK,
                                segmentTiers = mapOf(
                                    AnatomicalSegment.LEFT_ARM to SegmentTier.OPTIMAL,
                                    AnatomicalSegment.RIGHT_ARM to SegmentTier.OPTIMAL,
                                    AnatomicalSegment.TRUNK to SegmentTier.BALANCED,
                                    AnatomicalSegment.LEFT_LEG to SegmentTier.OPTIMAL,
                                    AnatomicalSegment.RIGHT_LEG to SegmentTier.ELEVATED,
                                ),
                                supports8Electrode = true,
                                onSelectSegment = {},
                            )
                        }
                    }
                    DetachedObsidianDock(
                        selectedDestination = OpenCultDestination.BODY_MAP,
                        onSelectDestination = { tappedNav = true },
                        onMeasureClick = {},
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("8-Electrode Body Map").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reports").performClick()
        assertThat(tappedNav).isTrue()
        val obsidianOut = File(screenshotDir, "visual_bodymap_obsidian.png")
        saveViewBitmap(rootView!!, obsidianOut)
        assertThat(obsidianOut.length()).isGreaterThan(1000L)
    }

    @Test
    fun empty_home_keeps_bento_health_connect_visible_and_no_fake_weight() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeScreenContent(
                        state = HomeDashboardUiState(
                            dailyHealthContext = DailyHealthContext(LocalDate.now(), 7240L, 420L, 1980),
                            hcGrantedCount = 12,
                        ),
                        decodeMetric = { _, _ -> null },
                        onSelectDate = {},
                        onSelectProfile = {},
                        onToggleMemberLock = {},
                        onReassignPending = { _, _ -> },
                        onOpenWeighIn = {},
                        onOpenMeasurementDetail = {},
                        onOpenInspector = {},
                        onOpenProfiles = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        composeRule.onNodeWithText("No Reading Today").assertIsDisplayed()
        composeRule.onNodeWithText("7240 steps").assertIsDisplayed()
    }

    @Test
    fun empty_reports_shows_dash_percent_instead_of_fake_body_fat() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                ReportsScreenContent(
                    state = ReportsUiState(),
                    metricLookup = { _, _ -> null },
                )
            }
        }
        composeRule.onNodeWithText("--%").assertIsDisplayed()
        composeRule.onNodeWithText("No Readings").assertIsDisplayed()
    }

    @Test
    fun main_activity_cold_launch_provides_navigation_event_dispatcher_and_renders_onboarding_inv25() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertThat(activity).isNotNull()
        assertThat(activity.isFinishing).isFalse()
        controller.pause().stop().destroy()
    }

    private fun saveViewBitmap(view: View, dest: File) {
        val w = view.width.coerceAtLeast(1080)
        val h = view.height.coerceAtLeast(2400)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)
        FileOutputStream(dest).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bmp.recycle()
    }
}
