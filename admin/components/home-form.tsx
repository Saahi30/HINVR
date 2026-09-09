"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { PhotoField } from "@/components/photo-field";
import { Alert, Button, Card, Field, fieldClass } from "@/components/ui";
import { slugify } from "@/lib/slug";
import { createClient } from "@/lib/supabase/client";
import { SERVICE_ROUTES, SERVICE_SCENES, type HomeService, type HomeSettings } from "@/lib/types";

export function HomeForm({
  initialServices,
  initialHome,
}: {
  initialServices: HomeService[];
  initialHome: HomeSettings;
}) {
  const router = useRouter();
  const [services, setServices] = useState(initialServices);
  const [removed, setRemoved] = useState<string[]>([]);
  const [home, setHome] = useState(initialHome);
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);

  function patchService(id: string, next: Partial<HomeService>) {
    setServices((rows) => rows.map((row) => (row.id === id ? { ...row, ...next } : row)));
  }

  function addTile() {
    const base = `tile-${services.length + 1}`;
    let id = slugify(base) || `tile-${Date.now()}`;
    if (services.some((row) => row.id === id)) id = `${id}-${Date.now()}`;
    const maxSort = services.reduce((max, row) => Math.max(max, row.sort_order), 0);
    setServices((rows) => [
      ...rows,
      {
        id,
        title: "New tile",
        benefit: "",
        photo_url: "",
        route: "live",
        scene: "LiveAarti",
        tall: false,
        sort_order: maxSort + 10,
        published: true,
      },
    ]);
  }

  function removeTile(id: string) {
    if (!confirm("Remove this home tile from the app?")) return;
    setServices((rows) => rows.filter((row) => row.id !== id));
    if (initialServices.some((row) => row.id === id)) {
      setRemoved((ids) => [...ids, id]);
    }
  }

  function moveTile(id: string, direction: -1 | 1) {
    setServices((rows) => {
      const index = rows.findIndex((row) => row.id === id);
      const next = index + direction;
      if (index < 0 || next < 0 || next >= rows.length) return rows;
      const copy = [...rows];
      const [item] = copy.splice(index, 1);
      copy.splice(next, 0, item);
      return copy.map((row, sortIndex) => ({ ...row, sort_order: (sortIndex + 1) * 10 }));
    });
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    setOk(false);
    try {
      const supabase = createClient();
      if (removed.length > 0) {
        const { error } = await supabase.from("home_services").delete().in("id", removed);
        if (error) throw error;
      }
      if (services.length > 0) {
        const { error } = await supabase.from("home_services").upsert(services);
        if (error) throw error;
      }
      const { error: homeError } = await supabase.from("app_settings").upsert({
        key: "home",
        value: home,
      });
      if (homeError) throw homeError;
      setRemoved([]);
      setOk(true);
      setMessage("Saved. The Android app picks this up on the next launch.");
      router.refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not save.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <Card className="space-y-4 p-5">
        <h2 className="text-sm font-semibold text-zinc-900">Hero copy</h2>
        <Field label="Eyebrow">
          <input
            value={home.eyebrow}
            onChange={(event) => setHome((current) => ({ ...current, eyebrow: event.target.value }))}
            className={fieldClass}
          />
        </Field>
        <Field label="Headline">
          <textarea
            rows={3}
            value={home.headline}
            onChange={(event) => setHome((current) => ({ ...current, headline: event.target.value }))}
            className={fieldClass}
          />
        </Field>
      </Card>
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-zinc-900">Service tiles</h2>
        <Button type="button" variant="secondary" onClick={addTile}>
          Add tile
        </Button>
      </div>
      {services.length === 0 ? (
        <Card className="p-8 text-center text-sm text-zinc-500">
          No tiles. The home grid will be empty until you add one.
        </Card>
      ) : (
        <section className="grid gap-4 xl:grid-cols-2">
          {services.map((service) => (
            <Card key={service.id} className="space-y-3 p-5">
              <div className="flex items-center justify-between gap-3">
                <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">{service.id}</p>
                <div className="flex items-center gap-1">
                  <Button type="button" variant="ghost" onClick={() => moveTile(service.id, -1)}>
                    Up
                  </Button>
                  <Button type="button" variant="ghost" onClick={() => moveTile(service.id, 1)}>
                    Down
                  </Button>
                  <Button type="button" variant="ghost" onClick={() => removeTile(service.id)}>
                    Remove
                  </Button>
                </div>
              </div>
              <Field label="Title">
                <input
                  value={service.title}
                  onChange={(event) => patchService(service.id, { title: event.target.value })}
                  className={fieldClass}
                />
              </Field>
              <Field label="Benefit line">
                <input
                  value={service.benefit}
                  onChange={(event) => patchService(service.id, { benefit: event.target.value })}
                  className={fieldClass}
                />
              </Field>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Opens">
                  <select
                    value={service.route}
                    onChange={(event) => patchService(service.id, { route: event.target.value })}
                    className={fieldClass}
                  >
                    {SERVICE_ROUTES.map((route) => (
                      <option key={route.value} value={route.value}>
                        {route.label}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Fallback art">
                  <select
                    value={service.scene}
                    onChange={(event) => patchService(service.id, { scene: event.target.value })}
                    className={fieldClass}
                  >
                    {SERVICE_SCENES.map((scene) => (
                      <option key={scene}>{scene}</option>
                    ))}
                  </select>
                </Field>
              </div>
              <PhotoField
                label="Tile photo"
                value={service.photo_url}
                folder={`home/${service.id}`}
                onChange={(photo_url) => patchService(service.id, { photo_url })}
              />
              <div className="flex flex-wrap gap-4 text-sm text-zinc-700">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={service.published}
                    onChange={(event) => patchService(service.id, { published: event.target.checked })}
                  />
                  Published
                </label>
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={service.tall}
                    onChange={(event) => patchService(service.id, { tall: event.target.checked })}
                  />
                  Tall card
                </label>
                <label className="flex items-center gap-2">
                  Sort
                  <input
                    type="number"
                    value={service.sort_order}
                    onChange={(event) => patchService(service.id, { sort_order: Number(event.target.value) })}
                    className="w-20 rounded-md border border-zinc-200 px-2 py-1 text-sm"
                  />
                </label>
              </div>
            </Card>
          ))}
        </section>
      )}
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Button type="submit" disabled={busy}>
        {busy ? "Saving…" : "Save home"}
      </Button>
    </form>
  );
}
