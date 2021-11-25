package buzz.getcoco.media.sample.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import buzz.getcoco.media.MediaSession;
import buzz.getcoco.media.sample.databinding.LayoutSessionsBinding;
import com.google.common.collect.ImmutableList;

/**
 * An adapter for showing sessions list.
 */
public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.ViewHolder> {

  private static final String TAG = "SessionsAdapter";

  private final OnClickListener listener;

  private ImmutableList<MediaSession.SessionHandle> sessions;

  public SessionsAdapter(OnClickListener listener) {
    this.listener = listener;
    this.sessions = ImmutableList.of();
  }

  public void setSessions(ImmutableList<MediaSession.SessionHandle> sessions) {
    this.sessions = sessions;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    return new ViewHolder(LayoutSessionsBinding.inflate(inflater, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    MediaSession.SessionHandle handle = sessions.get(position);

    holder.binding.tvSessionName.setText(handle.getName());
    holder.binding.tvSessionId.setText(handle.getId());
    holder.binding.tvSessionMetadata.setText(handle.getMetadata());

    holder.binding.getRoot().setOnClickListener(v -> {
      Log.d(TAG, "onBindViewHolder: session clicked: " + handle);
      listener.onClick(handle);
    });

    holder.binding.btnDelete.setOnClickListener(v -> {
      Log.d(TAG, "onBindViewHolder: delete clicked: " + handle);
      listener.onDelete(handle);
    });
  }

  @Override
  public int getItemCount() {
    return sessions.size();
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    private final LayoutSessionsBinding binding;

    public ViewHolder(@NonNull LayoutSessionsBinding binding) {
      super(binding.getRoot());

      this.binding = binding;
    }
  }

  /**
   * A listener for detecting clicks on view holders.
   */
  public interface OnClickListener {

    void onClick(MediaSession.SessionHandle handle);

    void onDelete(MediaSession.SessionHandle handle);
  }
}
