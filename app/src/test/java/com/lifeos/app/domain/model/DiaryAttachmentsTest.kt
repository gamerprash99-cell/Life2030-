package com.lifeos.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The attachments column is the one piece of Diary state that is not a plain
 * entity field, so its codec gets direct JVM coverage. Two properties matter:
 *  - a round trip must preserve every attachment exactly, and
 *  - it must never throw. A diary entry is the user's own words; losing or
 *    crashing on the whole row because a payload is corrupt is unacceptable,
 *    so an unreadable payload degrades to "no attachments".
 */
class DiaryAttachmentsTest {

    @Test
    fun encodeDecode_roundTripsEveryAttachmentKind() {
        val original = listOf(
            DiaryAttachment.Photo(filePath = "/data/user/0/com.lifeos.app/files/diary/1.jpg"),
            DiaryAttachment.VoiceNote(
                filePath = "/data/user/0/com.lifeos.app/files/diary/1.m4a",
                durationMillis = 18_400L
            ),
            DiaryAttachment.Place(
                latitude = 51.5074,
                longitude = -0.1278,
                placeName = "London",
                accuracyMeters = 12f
            )
        )

        val decoded = DiaryAttachments.decode(DiaryAttachments.encode(original))

        assertEquals(3, decoded.size)
        assertEquals(original, decoded)
    }

    /**
     * The polymorphic tag must stay stable and lowercase, or rows written by an
     * earlier build stop being readable.
     */
    @Test
    fun encode_usesStableKindDiscriminator() {
        val json = DiaryAttachments.encode(
            listOf(
                DiaryAttachment.Photo(filePath = "/a.jpg"),
                DiaryAttachment.VoiceNote(filePath = "/a.m4a", durationMillis = 1L),
                DiaryAttachment.Place(latitude = 0.0, longitude = 0.0)
            )
        )

        assertTrue("photo tag missing: $json", json.contains("\"kind\":\"photo\""))
        assertTrue("voice tag missing: $json", json.contains("\"kind\":\"voice\""))
        assertTrue("place tag missing: $json", json.contains("\"kind\":\"place\""))
    }

    @Test
    fun decode_emptyAndNullPayloadsYieldNoAttachments() {
        assertEquals(emptyList<DiaryAttachment>(), DiaryAttachments.decode(null))
        assertEquals(emptyList<DiaryAttachment>(), DiaryAttachments.decode(""))
        assertEquals(emptyList<DiaryAttachment>(), DiaryAttachments.decode("   "))
        // An entry with nothing attached stores the shortest legal payload.
        assertEquals("[]", DiaryAttachments.encode(emptyList()))
    }

    /** A corrupt payload must degrade to empty, never crash the entry. */
    @Test
    fun decode_corruptPayloadDegradesToEmpty() {
        assertEquals(emptyList<DiaryAttachment>(), DiaryAttachments.decode("{not json"))
        assertEquals(emptyList<DiaryAttachment>(), DiaryAttachments.decode("[{\"kind\":\"photo\""))
    }

    /** Forward compatibility: a field added by a later build must not break us. */
    @Test
    fun decode_ignoresUnknownFields() {
        val json = """[{"kind":"photo","filePath":"/a.jpg","aFieldFromTheFuture":42}]"""

        val decoded = DiaryAttachments.decode(json)

        assertEquals(1, decoded.size)
        assertEquals("/a.jpg", (decoded.first() as DiaryAttachment.Photo).filePath)
    }

    @Test
    fun accessors_pickTheRightAttachmentOutOfAMixedList() {
        val list = listOf(
            DiaryAttachment.Photo(filePath = "/1.jpg"),
            DiaryAttachment.Photo(filePath = "/2.jpg"),
            DiaryAttachment.VoiceNote(filePath = "/a.m4a", durationMillis = 5_000L),
            DiaryAttachment.VoiceNote(filePath = "/b.m4a", durationMillis = 9_000L),
            DiaryAttachment.Place(latitude = 1.0, longitude = 2.0, placeName = "First"),
            DiaryAttachment.Place(latitude = 3.0, longitude = 4.0, placeName = "Second")
        )

        assertEquals(2, DiaryAttachments.photos(list).size)
        // An entry supports at most one of each, so the newest wins.
        assertEquals("/b.m4a", DiaryAttachments.voiceNote(list)?.filePath)
        assertEquals("Second", DiaryAttachments.place(list)?.placeName)
    }

    @Test
    fun accessors_returnNullWhenTheKindIsAbsent() {
        val photosOnly = listOf(DiaryAttachment.Photo(filePath = "/1.jpg"))

        assertNull(DiaryAttachments.voiceNote(photosOnly))
        assertNull(DiaryAttachments.place(photosOnly))
    }

    /** A place with a blank name still has usable coordinates. */
    @Test
    fun place_survivesWithoutAResolvedName() {
        val decoded = DiaryAttachments.decode(
            DiaryAttachments.encode(
                listOf(DiaryAttachment.Place(latitude = 10.0, longitude = 20.0, placeName = ""))
            )
        )

        val place = decoded.first() as? DiaryAttachment.Place
        assertNotNull(place)
        assertEquals("", place!!.placeName)
        assertEquals(10.0, place.latitude, 0.0)
    }
}
