package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public class TextPreviewTest {
	TextPreviewGenerator textPreviewGenerator;
	String testInputString = "This is the worst kind of discrimination: the kind against me! I've been there. My folks were always on me to groom myself and wear underpants. What am I, the pope? Yep, I remember. They came in last at the Olympics, then retired to promote alcoholic beverages! I videotape every customer that comes in here, so that I may blackmail them later. No, of course not. It was… uh… porno. Yeah, that's it.\n\nIt may comfort you to know that Fry's death took only fifteen seconds, yet the pain was so intense, that it felt to him like fifteen years. And it goes without saying, it caused him to empty his bowels. Calculon is gonna kill us and it's all everybody else's fault! I just told you! You've killed me!\n\nBender?! You stole the atom. Guards! Bring me the forms I need to fill out to have her taken away! Bender, this is Fry's decision… and he made it wrong. So it's time for us to interfere in his life. A true inspiration for the children.\n\nIt's okay, Bender. I like cooking too. Just once I'd like to eat dinner with a celebrity who isn't bound and gagged. We'll go deliver this crate like professionals, and then we'll go home.\n\nYou can see how I lived before I met you. You mean while I'm sleeping in it? You, minion. Lift my arm. AFTER HIM! Now, now. Perfectly symmetrical violence never solved anything. Bite my shiny metal ass. Oh, but you can. But you may have to metaphorically make a deal with the devil. And by \"devil\", I mean Robot Devil. And by \"metaphorically\", I mean get your coat.\n\nNow what? Man, I'm sore all over. I feel like I just went ten rounds with mighty Thor. Perhaps, but perhaps your civilization is merely the sewer of an even greater society above you! Bender, hurry! This fuel's expensive! Also, we're dying!\n\nSay it in Russian! And from now on you're all named Bender Jr. You know, I was God once. WINDMILLS DO NOT WORK THAT WAY! GOOD NIGHT! Ven ve voke up, ve had zese wodies. And I'm his friend Jesus.\n\nI guess if you want children beaten, you have to do it yourself. You seem malnourished. Are you suffering from intestinal parasites? Large bet on myself in round one.\n\nYou're going to do his laundry? I've got to find a way to escape the horrible ravages of youth. Suddenly, I'm going to the bathroom like clockwork, every three hours. And those jerks at Social Security stopped sending me checks. Now 'I'' have to pay ''them'! Ow, my spirit! Can I use the gun? They're like sex, except I'm having them!\n\nFive hours? Aw, man! Couldn't you just get me the death penalty? Fry, you can't just sit here in the dark listening to classical music. We'll go deliver this crate like professionals, and then we'll go home.\n\nThis is the worst kind of discrimination: the kind against me! I found what I need. And it's not friends, it's things. I meant 'physically'. Look, perhaps you could let me work for a little food? I could clean the floors or paint a fence, or service you sexually? Who are those horrible orange men? Now, now. Perfectly symmetrical violence never solved anything.\n\nThe key to victory is discipline, and that means a well made bed. You will practice until you can make your bed in your sleep. Tell her she looks thin. No, of course not. It was… uh… porno. Yeah, that's it. Take me to your leader!\n\nUmmm…to eBay? We can't compete with Mom! Her company is big and evil! Ours is small and neutral! THE BIG BRAIN AM WINNING AGAIN! I AM THE GREETEST! NOW I AM LEAVING EARTH, FOR NO RAISEN!\n\nThen throw her in the laundry room, which will hereafter be referred to as \"the brig\". Robot 1-X, save my friends! And Zoidberg! Oh no! The professor will hit me! But if Zoidberg 'fixes' it… then perhaps gifts! Oh, I don't have time for this. I have to go and buy a single piece of fruit with a coupon and then return it, making people wait behind me while I complain. You're going back for the Countess, aren't you? Actually, that's still true.\n\nWhy would a robot need to drink? Michelle, I don't regret this, but I both rue and lament it. Is the Space Pope reptilian!?\n\nA sexy mistake. All I want is to be a monkey of moderate intelligence who wears a suit… that's why I'm transferring to business school! One hundred dollars. Why am I sticky and naked? Did I miss something fun?\n\nWhy not indeed! Well, let's just dump it in the sewer and say we delivered it. Have you ever tried just turning off the TV, sitting down with your children, and hitting them?\n\nI didn't ask for a completely reasonable excuse! I asked you to get busy! Morbo can't understand his teleprompter because he forgot how you say that letter that's shaped like a man wearing a hat. Kids don't turn rotten just from watching TV. Hey! I'm a porno-dealing monster, what do I care what you think? Check it out, y'all. Everyone who was invited is here. You, a bobsleder!? That I'd like to see!";
	InputStream from;
	@Before
	public void before() throws IOException, ServiceUnavailableException{
		from = IOUtils.toInputStream(testInputString);
		textPreviewGenerator = new TextPreviewGenerator();
	}
	
	@Test
	public void testGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata type = textPreviewGenerator.generatePreview(from, baos);
		assertEquals(TextPreviewGenerator.TEXT_PLAIN, type.getContentType());
		String output = baos.toString();
		assertTrue(output.length()<testInputString.length());
		assertTrue(output.indexOf("...") > -1);
	}

	@Test
	public void testContentType() throws IOException {
		assertTrue(textPreviewGenerator.supportsContentType("text/xml"));
		assertTrue(textPreviewGenerator.supportsContentType("text/html"));
		assertTrue(textPreviewGenerator.supportsContentType(TextPreviewGenerator.APPLICATION_JS));
		assertTrue(textPreviewGenerator.supportsContentType(TextPreviewGenerator.APPLICATION_SH));
		assertFalse(textPreviewGenerator.supportsContentType("image/anything"));
		assertFalse(textPreviewGenerator.supportsContentType("csv"));
		assertFalse(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES));
		assertFalse(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES));
	}
}
