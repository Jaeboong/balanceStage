import argparse, json, sys, numpy as np
import matplotlib
matplotlib.use("Agg")  # 창 없이 저장
import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
from matplotlib.cm import ScalarMappable, get_cmap

def pick_sigma_from_points(x, y, scale=0.35):
    n = len(x); dmin = float("inf")
    for i in range(n):
        for j in range(i+1, n):
            dx = x[i]-x[j]; dy = y[i]-y[j]
            d = (dx*dx + dy*dy) ** 0.5
            if d < dmin: dmin = d
    return max(dmin * scale, 1e-6)

def rbf_gaussian_weights(x, y, v, sigma, lam=1e-9):
    X = np.column_stack([x, y])
    d2 = np.sum((X[:,None,:]-X[None,:,:])**2, axis=2)
    K = np.exp(-d2/(sigma**2))
    K[np.diag_indices_from(K)] += lam
    return np.linalg.solve(K, v)

def rbf_gaussian_eval(w, x_pts, y_pts, Xg, Yg, sigma):
    Zg = np.zeros_like(Xg, dtype=float)
    for (xj, yj), wj in zip(zip(x_pts, y_pts), w):
        d2 = (Xg - xj)**2 + (Yg - yj)**2
        Zg += wj * np.exp(-d2/(sigma**2))
    return Zg

def plot_rect_surface(points, sigma_scale, lam, grid_res, cmap_name, n_contours,
                      force_vmin_zero=True, center_origin=True, pad_ratio=0.1):
    x, y, v = points[:,0], points[:,1], points[:,3].astype(float)
    xmin, xmax = x.min(), x.max()
    ymin, ymax = y.min(), y.max()

    if center_origin:
        Rx = max(abs(xmin), abs(xmax)) * (1 + pad_ratio)
        Ry = max(abs(ymin), abs(ymax)) * (1 + pad_ratio)
        xi = np.linspace(-Rx, Rx, grid_res)
        yi = np.linspace(-Ry, Ry, grid_res)
    else:
        dx = (xmax - xmin) * pad_ratio
        dy = (ymax - ymin) * pad_ratio
        xi = np.linspace(xmin - dx, xmax + dx, grid_res)
        yi = np.linspace(ymin - dy, ymax + dy, grid_res)

    Xg, Yg = np.meshgrid(xi, yi)

    sigma = pick_sigma_from_points(x, y, scale=sigma_scale)
    w = rbf_gaussian_weights(x, y, v, sigma=sigma, lam=lam)
    Zg = rbf_gaussian_eval(w, x, y, Xg, Yg, sigma=sigma)

    if force_vmin_zero:
        vmin = 0.0
        vmax = float(np.nanmax(v))
        if vmax <= vmin: vmax = vmin + 1.0
    else:
        vmin, vmax = float(np.nanmin(Zg)), float(np.nanmax(Zg))
        if vmax - vmin < 1e-12: vmax = vmin + 1.0

    cmap = get_cmap(cmap_name)
    norm = Normalize(vmin=vmin, vmax=vmax)
    facecolors = cmap(norm(Zg))

    fig = plt.figure(figsize=(9,7))
    ax = fig.add_subplot(111, projection="3d")
    ax.plot_surface(Xg, Yg, Zg, rstride=1, cstride=1, facecolors=facecolors,
                    linewidth=0, antialiased=True, shade=False)
    try:
        ax.contour3D(Xg, Yg, Zg, levels=n_contours, linewidths=0.6, colors="k", alpha=0.45)
    except Exception:
        pass

    ax.scatter(x, y, v, s=90, c="k", depthshade=False, label="measured")
    ax.set_xlabel("X"); ax.set_ylabel("Y"); ax.set_zlabel("devi")
    ax.set_title("Balance Stage Surface (Rectangular domain)")
    try:
        ax.set_box_aspect((xi.ptp(), yi.ptp(), max(vmax - vmin, 1.0)))
    except Exception:
        pass
    ax.view_init(elev=35, azim=-55)

    mappable = ScalarMappable(norm=norm, cmap=cmap)
    mappable.set_array(Zg)
    cbar = fig.colorbar(mappable, ax=ax, shrink=0.7, pad=0.1)
    cbar.set_label("devi")
    ax.legend(loc="upper left")
    plt.tight_layout()
    return fig

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="in_json", required=True)
    ap.add_argument("--out", dest="out_png", required=True)
    ap.add_argument("--sigma", type=float, default=0.50)
    ap.add_argument("--grid", type=int, default=260)
    ap.add_argument("--cmap", type=str, default="turbo")
    ap.add_argument("--contours", type=int, default=24)
    args = ap.parse_args()

    with open(args.in_json, "r", encoding="utf-8") as f:
        arr = np.array(json.load(f), dtype=float)

    fig = plot_rect_surface(arr,
                            sigma_scale=args.sigma,
                            lam=1e-9,
                            grid_res=args.grid,
                            cmap_name=args.cmap,
                            n_contours=args.contours,
                            force_vmin_zero=True,
                            center_origin=True,
                            pad_ratio=0.1)
    fig.savefig(args.out_png, dpi=120)
    plt.close(fig)
    print("SAVED:", args.out_png)
    return 0

if __name__ == "__main__":
    sys.exit(main())
