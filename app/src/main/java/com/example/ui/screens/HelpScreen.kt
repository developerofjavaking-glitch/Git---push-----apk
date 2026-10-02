package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GitGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Guide & Help / সাহায্য নির্দেশিকা",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Button: Generate Token
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/settings/tokens/new?scopes=repo&description=GitPusher-Android-App")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate GitHub Token / টোকেন তৈরি করুন", fontWeight = FontWeight.Bold)
            }

            // Step-by-Step Guide Card (Bengali & English)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কিভাবে গিটহাব টোকেন (PAT) পাবেন?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "How to generate a Personal Access Token",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HelpStepItem(
                        step = "1",
                        title = "গিটহাবে লগইন করুন",
                        detail = "আপনার ব্রাউজারে GitHub.com এ যান এবং Settings > Developer Settings > Personal access tokens এ যান।"
                    )

                    HelpStepItem(
                        step = "2",
                        title = "Tokens (classic) নির্বাচন করুন",
                        detail = "Generate new token (classic) এ চাপ দিন এবং Note এর জায়গায় নাম দিন (যেমন: GitPusher)।"
                    )

                    HelpStepItem(
                        step = "3",
                        title = "'repo' পারমিশন টিক চিহ্ন দিন",
                        detail = "Select scopes সেকশনে 'repo' (Full control of private repositories) চেক করুন। ফাইল পুশ ও রিড করতে এটি বাধ্যতামূলক।"
                    )

                    HelpStepItem(
                        step = "4",
                        title = "টোকেন কপি করে অ্যাপে পেস্ট করুন",
                        detail = "Generate Token বাটনে চাপ দিন এবং ghp_ দিয়ে শুরু হওয়া কোডটি কপি করে এই অ্যাপের টোকেন বক্সে পেস্ট করে 'Verify & Save' করুন।"
                    )
                }
            }

            // How Folder Upload Works
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ফোন থেকে ফোল্ডার পুশ করার নিয়ম",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• ফোন থেকে যেকোনো ফোল্ডার সিলেক্ট করলেই সেই ফোল্ডারের ভিতরের সব ফাইল এবং সাব-ফোল্ডারের ফাইল স্বয়ংক্রিয়ভাবে স্ক্যান হবে।",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• গিটহাবের Git Tree & Blob ইঞ্জিনের মাধ্যমে এক ক্লিকেই একটি ক্লিন সিঙ্গেল কমিটে পুরো ফোল্ডারটি পুশ হয়ে যাবে।",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• আপনার তৈরি করা যেকোনো রিপোজিটরিতে (বা নতুন রিপোজিটরি বানিয়ে) পুশ করতে পারবেন।",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Security & Privacy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = GitGreenLight,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "নিরাপত্তা ও প্রাইভেসি (100% Secure)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "আপনার এক্সেস টোকেন শুধুমাত্র আপনার ফোনের সুরক্ষিত লোকাল ডেটাবেজে সংরক্ষিত থাকে। কোনো থার্ড-পার্টি সার্ভারে পাঠানো হয় না, সরাসরি GitHub API-তে কাজ করে।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HelpStepItem(step: String, title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = detail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
