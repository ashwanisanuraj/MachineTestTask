package com.xero.machinetesttask

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.Random

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userDetailTextView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Set the listener for refresh action
        swipeRefreshLayout.setOnRefreshListener {
            // Call your data retrieval method here
            retrieveData()
        }

        auth = Firebase.auth
        databaseReference = FirebaseDatabase.getInstance().reference.child("user_logins")
        userDetailTextView = findViewById(R.id.userDetail)

        val currentUser = auth.currentUser
        currentUser?.let {
            userDetailTextView.text = "Logged in With: ${it.phoneNumber}"
            retrieveLoginData(it.uid)
        }

        val buttonLogout = findViewById<Button>(R.id.buttonLogout)
        buttonLogout.setOnClickListener {
            signOut()
        }

        // Retrieve login data for all users
        retrieveLoginData2()
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, PhoneNumberActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun retrieveData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            retrieveLoginData(userId)
            retrieveLoginData2() // Retrieve data for the second graph as well
        }

        // After data retrieval is complete, end the refreshing animation
        swipeRefreshLayout.isRefreshing = false
        Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
    }


private fun retrieveLoginData(userId: String) {
        val userLoginRef = databaseReference.child(userId)

        userLoginRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val loginCountsPerMonth = mutableMapOf<Int, Int>()

                    for (loginSnapshot in dataSnapshot.children) {
                        val month = loginSnapshot.child("month").getValue(Int::class.java)
                        month?.let {
                            val count = loginCountsPerMonth[it] ?: 0
                            loginCountsPerMonth[it] = count + 1
                        }
                    }
                    prepareBarGraphData(loginCountsPerMonth)
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "No login data available for current user",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@HomeActivity,
                    "Failed to retrieve login data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    private fun prepareBarGraphData(loginCountsPerMonth: Map<Int, Int>) {
        val entries = ArrayList<BarEntry>()

        for ((month, count) in loginCountsPerMonth) {
            val xValue = month.toFloat() - 0.5f // Adjust xValue by 0.5f for shifting
            entries.add(BarEntry(xValue, count.toFloat()))
        }

        val barDataSet = BarDataSet(entries, "Login Count per Month")
        val barData = BarData(barDataSet)

        displayBarGraph(barData)
    }
    private fun displayBarGraph(barData: BarData) {
        val barChart = findViewById<BarChart>(R.id.barChart)
        barChart.data = barData

        barData.barWidth = 1f // Adjust bar width as needed

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(
            arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
        )
        xAxis.textColor = Color.BLUE
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
        xAxis.labelCount = 12
        xAxis.setCenterAxisLabels(true)
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 12f

        val yAxis = barChart.axisLeft
        yAxis.textColor = Color.BLUE
        yAxis.granularity = 1f
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = barData.yMax + 1f // Adjust headroom as needed
        yAxis.setLabelCount((yAxis.axisMaximum - yAxis.axisMinimum).toInt() + 1, true)

        val rightYAxis = barChart.axisRight
        rightYAxis.isEnabled = false

        barChart.extraBottomOffset = 10f
        barChart.setFitBars(true)
        barChart.description.isEnabled = false // Hide the description label
        barChart.invalidate()
    }
    private fun retrieveLoginData2() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val loginData = mutableMapOf<Int, MutableMap<String, Int>>()
                val userPhoneMap = mutableMapOf<String, String>()

                for (userSnapshot in dataSnapshot.children) {
                    val userId = userSnapshot.key ?: continue

                    // Check if the user node has "phoneNumber" child
                    if (userSnapshot.hasChild("phoneNumber")) {
                        val phoneNumber = userSnapshot.child("phoneNumber").getValue(String::class.java)
                        if (phoneNumber != null) {
                            userPhoneMap[userId] = phoneNumber
                        } else {
                            Log.w("HomeActivity", "Phone number missing for user: $userId") // Log a warning
                            userPhoneMap[userId] = userId // Use userId as fallback
                        }
                    } else {
                        Log.w("HomeActivity", "User $userId missing 'phoneNumber' field")
                    }

                    // Process login data for the user
                    for (loginSnapshot in userSnapshot.children) {
                        val month = loginSnapshot.child("month").getValue(Int::class.java)
                        month?.let {
                            val userLoginCounts = loginData[it] ?: mutableMapOf()
                            userLoginCounts[userId] = (userLoginCounts[userId] ?: 0) + 1
                            loginData[it] = userLoginCounts
                        }
                    }
                }

                // Log all the retrieved data
                Log.d("HomeActivity", "Retrieved login data: $loginData")
                Log.d("HomeActivity", "Retrieved user phone map: $userPhoneMap")

                // Pass data to prepareStackedBarGraphData
                prepareStackedBarGraphData(loginData, userPhoneMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@HomeActivity,
                    "Failed to retrieve login data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    private fun prepareStackedBarGraphData(loginData: Map<Int, Map<String, Int>>, userPhoneMap: Map<String, String>) {
        val entries = ArrayList<BarEntry>()
        val userColors = mutableMapOf<String, Int>()
        val random = Random()

        loginData.forEach { (month, userCounts) ->
            val stackValues = FloatArray(userCounts.size)
            var index = 0
            userCounts.forEach { (userId, count) ->
                if (!userColors.containsKey(userId)) {
                    userColors[userId] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                }
                stackValues[index++] = count.toFloat()
            }
            entries.add(BarEntry(month.toFloat() - 0.5f, stackValues)) // Shift by 0.5f
        }

        val barDataSet = BarDataSet(entries, "")
        barDataSet.colors = userColors.values.toList()

        // Replace userIds with phone numbers in stack labels
        val stackLabels = userColors.keys.map { userPhoneMap[it] ?: it }.toTypedArray()
        barDataSet.stackLabels = stackLabels

        val barData = BarData(barDataSet)
        displayStackedBarGraph(barData)
    }
    private fun displayStackedBarGraph(barData: BarData) {
        val barChartAll = findViewById<BarChart>(R.id.barChartAll)
        barChartAll.data = barData

        barData.barWidth = 1f // Adjust bar width as needed

        val xAxis = barChartAll.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(
            arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        )
        xAxis.textColor = Color.BLUE
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
        xAxis.labelCount = 12
        xAxis.setCenterAxisLabels(true) // Ensure labels are directly under the bars
        xAxis.axisMinimum = 0f // Start slightly before the first bar to align correctly
        xAxis.axisMaximum = 12f // Set maximum to display all twelve months

        // Customize Y Axis
        val yAxis = barChartAll.axisLeft
        yAxis.textColor = Color.BLUE
        yAxis.granularity = 1f
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = barData.yMax + 1f // Set maximum slightly above the highest value
        yAxis.setLabelCount((yAxis.axisMaximum - yAxis.axisMinimum).toInt() + 1, true)

        // Disable right Y Axis
        val rightYAxis = barChartAll.axisRight
        rightYAxis.isEnabled = false

        // Customize Legend
        val legend = barChartAll.legend
        legend.isEnabled = true
        legend.textSize = 9f
        legend.textColor = Color.BLACK
        legend.form = Legend.LegendForm.SQUARE
        legend.formSize = 10f
        legend.xEntrySpace = 2f // Decrease space between legend entries


        // Ensure proper legend position and offset
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        barChartAll.extraBottomOffset = 10f
        barChartAll.setFitBars(true)
        barChartAll.description.isEnabled = false // Disable description label
        barChartAll.invalidate() // Refresh chart
    }

}
