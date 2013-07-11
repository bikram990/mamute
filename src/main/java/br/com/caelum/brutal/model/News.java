package br.com.caelum.brutal.model;

import static javax.persistence.FetchType.EAGER;
import static org.hibernate.annotations.CascadeType.SAVE_UPDATE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import br.com.caelum.brutal.model.interfaces.Moderatable;
import br.com.caelum.brutal.model.interfaces.Votable;

@Entity
public class News extends Moderatable implements Post {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false, fetch = EAGER)
	@Cascade(SAVE_UPDATE)
	@NotNull
	private NewsInformation information = null;
	
	@OneToMany(mappedBy="news")
	@Cascade(SAVE_UPDATE)
	private List<NewsInformation> history = new ArrayList<>();
	
	@Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
	private final DateTime createdAt = new DateTime();

	@Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
	private DateTime lastUpdatedAt = new DateTime();

	@ManyToOne
	private User lastTouchedBy = null;

	@ManyToOne(fetch = EAGER)
	private final User author;

	private long views = 0l;
	
	@JoinTable(name = "News_Votes")
	@OneToMany
	private final List<Vote> votes = new ArrayList<>();

	private long voteCount = 0l;

	@Embedded
	private final NewsCommentList comments = new NewsCommentList();
	
	@Embedded
	private final ModerationOptions moderationOptions = new ModerationOptions();
	
	@JoinTable(name = "News_Flags")
	@OneToMany
	private final List<Flag> flags = new ArrayList<>();

	/**
	 * @deprecated hibernate eyes only
	 */
	public News() {
		author = null;
	}
	
	public News(NewsInformation newsInformation, User author) {
		this.author = author;
		enqueueChange(newsInformation, UpdateStatus.NO_NEED_TO_APPROVE);
	}

	@Override
	public void substitute(Vote previous, Vote current) {
		this.voteCount += current.substitute(previous, votes);
	}

	@Override
	public User getAuthor() {
		return author;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public long getVoteCount() {
		return voteCount;
	}

	@Override
	public Class<? extends Votable> getType() {
		return News.class;
	}

	@Override
	public Comment add(Comment comment) {
		comments.add(comment);
		return comment;
	}

	@Override
	public List<Comment> getVisibleCommentsFor(User user) {
		return comments.getVisibleCommentsFor(user);
	}

	@Override
	public DateTime getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	@Override
	public User getLastTouchedBy() {
		return lastTouchedBy;
	}

	@Override
	public DateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean isEdited() {
		return history.size() > 1;
	}

	@Override
	public boolean alreadyFlaggedBy(User user) {
		for (Flag flag : flags) {
			if (flag.createdBy(user))
				return true;
		}
		return false;
	}

	@Override
	public void remove() {
		moderationOptions.remove();
	}

	@Override
	public boolean isVisible() {
		return moderationOptions.isVisible();
	}

	@Override
	public boolean isVisibleForModeratorAndNotAuthor(User user) {
		return !this.isVisible() && user != null && !user.isAuthorOf(this);
	}
	
	@Override
	public String getTypeNameKey() {
		return ""; //TODO TypeNameKey
	}

	@Override
	protected NewsInformation getInformation() {
		return information;
	}

	@Override
	protected void updateApproved(Information approved) {
		NewsInformation approvedNews = (NewsInformation) approved;
		touchedBy(approvedNews.getAuthor());
		setInformation(approvedNews);		
	}

	private void setInformation(NewsInformation approvedNews) {
		approvedNews.setNews(this);
        this.information = approvedNews;		
	}

	private void touchedBy(User author) {
		this.lastTouchedBy = author;
		this.lastUpdatedAt = new DateTime();
	}

	@Override
	public String getTypeName() {
        return getType().getSimpleName();
    }
	
	@Override
	public boolean hasPendingEdits() {
		for (NewsInformation information : history) {
			if(information.isPending()) return true;
		}
		return false;
	}

	@Override
	public void add(Flag flag) {
		flags.add(flag);
	}

	@Override
	public Question getQuestion() {
		return null; //TODO: remove getQuestion from Post interface
	}
	
	public String getTitle(){
		return information.getTitle();
	}

	public UpdateStatus updateWith(NewsInformation information) {
	    return new Updater().update(this, information);
	}

	public List<NewsInformation> getHistory() {
		return history;
	}

	@Override
	protected void addHistory(Information information) {
		this.history.add((NewsInformation) information);
	}

	public String getSluggedTitle() {
		return information.getSluggedTitle();
	}
	
	public String getMarkedDescription(){
		return information.getMarkedDescription();
	}
	
	public long getViews() {
		return views;
	}
}

