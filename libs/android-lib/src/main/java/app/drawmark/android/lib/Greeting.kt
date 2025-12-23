package app.drawmark.android.lib

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A sample Composable from the android-lib library.
 *
 * @param name The name to greet.
 * @param modifier Optional modifier for the composable.
 */
@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Hello, $name!",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "This message comes from android-lib",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        Greeting("Drawmark User")
    }
}
